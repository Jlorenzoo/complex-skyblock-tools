package com.skyblocktoolkit;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class UpdateManager {
    public enum State {
        IDLE,
        CHECKING,
        UPDATE_AVAILABLE,
        UP_TO_DATE,
        DOWNLOADING,
        READY_RESTART,
        ERROR
    }

    private static final String DEFAULT_REPO = "Jlorenzoo/complex-skyblock-tools";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/" + DEFAULT_REPO + "/releases?per_page=30";

    private static volatile State state = State.IDLE;
    private static volatile String latestVersion = "";
    private static volatile String downloadUrl = "";
    private static volatile String releaseNotes = "";
    private static volatile String lastError = "";
    private static volatile boolean chatNotified = false;
    private static final Object LOCK = new Object();

    private UpdateManager() {}

    private static String minecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
            .orElseThrow().getMetadata().getVersion().getFriendlyString();
    }

    public static State getState() { return state; }
    public static String getLatestVersion() { return latestVersion; }
    public static String getDownloadUrl() { return downloadUrl; }
    public static String getLastError() { return lastError; }

    public static String getCurrentVersion() {
        return FabricLoader.getInstance().getModContainer("skyblocktoolkit")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("1.1.0");
    }

    public static boolean isUpdateAvailable() {
        return state == State.UPDATE_AVAILABLE || state == State.DOWNLOADING || state == State.READY_RESTART;
    }

    public static void checkForUpdatesAsync() {
        checkForUpdatesAsync(null);
    }

    public static void checkForUpdatesAsync(Runnable onComplete) {
        synchronized (LOCK) {
            if (state == State.CHECKING || state == State.DOWNLOADING) return;
            state = State.CHECKING;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String current = getCurrentVersion();
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(6))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GITHUB_API_URL))
                        .header("User-Agent", "SkyblockToolkit-Updater")
                        .header("Accept", "application/vnd.github.v3+json")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String version = current;
                    String body = "";
                    String assetUrl = "";
                    for (JsonElement release : JsonParser.parseString(response.body()).getAsJsonArray()) {
                        JsonObject json = release.getAsJsonObject();
                        if ((json.has("prerelease") && json.get("prerelease").getAsBoolean())
                            || (json.has("draft") && json.get("draft").getAsBoolean())) continue;
                        String candidate = UpdateChannel.releaseVersion(json.has("tag_name") ? json.get("tag_name").getAsString() : "");
                        if (candidate.isEmpty() || compareVersions(candidate, version) < 0) continue;
                        if (json.has("assets") && json.get("assets").isJsonArray()) {
                            for (JsonElement el : json.getAsJsonArray("assets")) {
                                if (!el.isJsonObject()) continue;
                                JsonObject asset = el.getAsJsonObject();
                                String name = asset.has("name") ? asset.get("name").getAsString() : "";
                                if (UpdateChannel.matches(name, candidate, minecraftVersion())) {
                                    assetUrl = asset.has("browser_download_url") ? asset.get("browser_download_url").getAsString() : "";
                                    version = candidate;
                                    body = json.has("body") ? json.get("body").getAsString() : "";
                                    break;
                                }
                            }
                        }
                    }

                    if (!assetUrl.isEmpty() && !assetUrl.startsWith("https://github.com/" + DEFAULT_REPO + "/releases/download/")) {
                        throw new SecurityException("Untrusted download URL domain: " + assetUrl);
                    }

                    synchronized (LOCK) {
                        latestVersion = version;
                        releaseNotes = body;
                        downloadUrl = assetUrl;

                        if (!version.isEmpty() && compareVersions(version, current) > 0 && !assetUrl.isEmpty()) {
                            state = State.UPDATE_AVAILABLE;
                        } else {
                            state = State.UP_TO_DATE;
                        }
                    }
                } else if (response.statusCode() == 404) {
                    synchronized (LOCK) {
                        state = State.ERROR;
                        lastError = "Repository or releases not found (HTTP 404). Ensure releases are public.";
                    }
                } else {
                    synchronized (LOCK) {
                        state = State.ERROR;
                        lastError = "GitHub returned HTTP " + response.statusCode();
                    }
                }
            } catch (Exception e) {
                synchronized (LOCK) {
                    state = State.ERROR;
                    lastError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                }
            } finally {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    public static void downloadAndInstallAsync() {
        synchronized (LOCK) {
            if (state == State.DOWNLOADING) return;
            if (state == State.READY_RESTART) return;
            if (downloadUrl.isEmpty() || latestVersion.isEmpty()) {
                sendFeedback("§cCannot update: No valid download URL found.");
                return;
            }
            state = State.DOWNLOADING;
        }

        sendFeedback("§8[§bSkyblock Toolkit§8] §7Downloading update §bv" + latestVersion + "§7...");

        CompletableFuture.runAsync(() -> {
            Path tempDownload = null;
            try {
                Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
                if (!Files.exists(modsDir)) {
                    Files.createDirectories(modsDir);
                }

                String jarName = UpdateChannel.jarName(latestVersion, minecraftVersion());
                tempDownload = modsDir.resolve(jarName + ".download");

                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .header("User-Agent", "SkyblockToolkit-Updater")
                        .timeout(Duration.ofMinutes(2))
                        .GET()
                        .build();

                HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() != 200) {
                    throw new IOException("Download failed with HTTP " + resp.statusCode());
                }

                try (InputStream in = resp.body(); OutputStream out = Files.newOutputStream(tempDownload, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                    in.transferTo(out);
                }

                // Verify downloaded file integrity
                if (Files.size(tempDownload) < 10_000) {
                    throw new IOException("Downloaded file is too small (" + Files.size(tempDownload) + " bytes).");
                }
                try (ZipFile zip = new ZipFile(tempDownload.toFile())) {
                    ZipEntry entry = zip.getEntry("fabric.mod.json");
                    if (entry == null) {
                        throw new IOException("Downloaded archive does not contain fabric.mod.json.");
                    }
                    try (Reader reader = new InputStreamReader(zip.getInputStream(entry), java.nio.charset.StandardCharsets.UTF_8)) {
                        JsonObject metadata = JsonParser.parseReader(reader).getAsJsonObject();
                        if (!metadata.get("id").getAsString().equals("skyblocktoolkit")
                            || !metadata.get("version").getAsString().equals(latestVersion)
                            || !net.fabricmc.loader.api.metadata.version.VersionPredicate.parse(
                                metadata.getAsJsonObject("depends").get("minecraft").getAsString())
                                .test(net.fabricmc.loader.api.Version.parse(minecraftVersion()))) {
                            throw new IOException("Downloaded toolkit does not match this Minecraft version.");
                        }
                    }
                }

                // Install the new jar into mods/
                Path targetJar = modsDir.resolve(jarName);
                Files.move(tempDownload, targetJar, StandardCopyOption.REPLACE_EXISTING);

                // Handle old jar cleanup safely
                Path currentJar = getCurrentJarPath();
                if (currentJar != null && Files.exists(currentJar)) {
                    boolean same = false;
                    try {
                        same = Files.isSameFile(currentJar, targetJar) || currentJar.toRealPath().equals(targetJar.toRealPath());
                    } catch (Exception ignored) {}
                    if (!same && !currentJar.equals(targetJar)) {
                        try {
                            Files.deleteIfExists(currentJar);
                        } catch (Exception ex) {
                            scheduleWindowsDeletion(currentJar);
                        }
                    }
                }

                synchronized (LOCK) {
                    state = State.READY_RESTART;
                }
                sendFeedback("§8[§bSkyblock Toolkit§8] §a§lUpdate installed! §fRestart Minecraft to activate §bv" + latestVersion + "§f.");
            } catch (Exception e) {
                if (tempDownload != null) {
                    try { Files.deleteIfExists(tempDownload); } catch (Exception ignored) {}
                }
                synchronized (LOCK) {
                    state = State.ERROR;
                    lastError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                }
                sendFeedback("§8[§bSkyblock Toolkit§8] §cUpdate installation failed: " + lastError);
            }
        });
    }

    public static void checkAndNotifyChat(LocalPlayer player) {
        if (player == null || chatNotified) return;
        if (state == State.UPDATE_AVAILABLE) {
            chatNotified = true;
            MutableComponent msg = Component.literal("§8[§bSkyblock Toolkit§8] §aA new update §e§lv" + latestVersion + "§a is available! ");
            MutableComponent button = Component.literal("§b§l[Click to Update]")
                    .withStyle(s -> s
                            .withColor(ChatFormatting.AQUA)
                            .withBold(true)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent.RunCommand("/toolkit update"))
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7Click to automatically download & install §bv" + latestVersion + "§7!")))
                    );
            msg.append(button);
            player.sendSystemMessage(msg);
        }
    }

    public static void triggerUpdateFromCommand() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        switch (state) {
            case DOWNLOADING -> mc.player.sendSystemMessage(Component.literal("§8[§bSkyblock Toolkit§8] §eAn update is already downloading..."));
            case READY_RESTART -> mc.player.sendSystemMessage(Component.literal("§8[§bSkyblock Toolkit§8] §aUpdate v" + latestVersion + " is installed! Restart Minecraft to apply."));
            case UPDATE_AVAILABLE -> downloadAndInstallAsync();
            case UP_TO_DATE -> mc.player.sendSystemMessage(Component.literal("§8[§bSkyblock Toolkit§8] §7You are running the latest version (§a" + getCurrentVersion() + "§7)."));
            case CHECKING -> mc.player.sendSystemMessage(Component.literal("§8[§bSkyblock Toolkit§8] §7Checking for updates, please wait..."));
            case IDLE, ERROR -> {
                mc.player.sendSystemMessage(Component.literal("§8[§bSkyblock Toolkit§8] §7Checking for updates..."));
                checkForUpdatesAsync(() -> {
                    Minecraft m = Minecraft.getInstance();
                    if (m.player == null) return;
                    if (state == State.UPDATE_AVAILABLE) {
                        checkAndNotifyChat(m.player);
                    } else if (state == State.UP_TO_DATE) {
                        m.player.sendSystemMessage(Component.literal("§8[§bSkyblock Toolkit§8] §7You are running the latest version (§a" + getCurrentVersion() + "§7)."));
                    } else if (state == State.ERROR) {
                        m.player.sendSystemMessage(Component.literal("§8[§bSkyblock Toolkit§8] §cUpdate check failed: " + lastError));
                    }
                });
            }
        }
    }

    public static void cleanOldBackups() {
        try {
            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
            if (!Files.isDirectory(modsDir)) return;
            Path currentJar = getCurrentJarPath();
            String currentVersion = getCurrentVersion();
            String activeJarName = UpdateChannel.jarName(currentVersion, minecraftVersion());
            try (var stream = Files.list(modsDir)) {
                for (Path p : stream.toList()) {
                    String name = p.getFileName().toString();
                    if (name.endsWith(".download") || name.endsWith(".pending") || name.contains(".bak.")) {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        continue;
                    }
                    if (name.equals(activeJarName)) {
                        // Current version jar: never delete under any circumstance
                        continue;
                    }
                    if (currentJar != null) {
                        try {
                            if (Files.isSameFile(p, currentJar) || p.toRealPath().equals(currentJar.toRealPath())) {
                                continue;
                            }
                        } catch (Exception ignored) {}
                    }
                    if (name.startsWith("skyblock-toolkit-") && name.endsWith(".jar")) {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static Path getCurrentJarPath() {
        return FabricLoader.getInstance().getModContainer("skyblocktoolkit")
                .map(c -> {
                    var paths = c.getOrigin().getPaths();
                    if (!paths.isEmpty()) {
                        Path p = paths.get(0);
                        if (Files.isRegularFile(p)) {
                            try { return p.toRealPath(); } catch (Exception e) { return p.toAbsolutePath().normalize(); }
                        }
                    }
                    return null;
                })
                .orElse(null);
    }

    private static void scheduleWindowsDeletion(Path jarToDelete) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String absPath = jarToDelete.toAbsolutePath().toString();
            if (absPath.contains("\"") || absPath.contains("&") || absPath.contains("|") || absPath.contains(";") || absPath.contains("%")) {
                jarToDelete.toFile().deleteOnExit();
                return;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    new ProcessBuilder("cmd.exe", "/c", "timeout /t 2 /nobreak >nul & del /f /q \"" + absPath + "\"").start();
                } catch (Exception ignored) {}
            }));
        }
    }

    private static void sendFeedback(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            mc.execute(() -> mc.player.sendSystemMessage(Component.literal(message)));
        }
    }

    public static int compareVersions(String v1, String v2) {
        if (v1 == null) return v2 == null ? 0 : -1;
        if (v2 == null) return 1;
        v1 = v1.trim().replaceFirst("^[vV]", "");
        v2 = v2.trim().replaceFirst("^[vV]", "");
        String[] parts1 = v1.split("[-+.]");
        String[] parts2 = v2.split("[-+.]");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            String p1 = i < parts1.length ? parts1[i] : "0";
            String p2 = i < parts2.length ? parts2[i] : "0";
            try {
                int n1 = Integer.parseInt(p1);
                int n2 = Integer.parseInt(p2);
                if (n1 != n2) return Integer.compare(n1, n2);
            } catch (NumberFormatException e) {
                int cmp = p1.compareToIgnoreCase(p2);
                if (cmp != 0) return cmp;
            }
        }
        return 0;
    }
}
