package com.moneybooster;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModConfig {
    public enum Position {
        TOP_CENTER,
        TOP_LEFT
    }

    private static final ModConfig INSTANCE = new ModConfig();
    private Position position = Position.TOP_CENTER;
    private boolean averageIncludeBoosters=true;
    public boolean includesBoostersInAverage(){return averageIncludeBoosters;}
    public void toggleAverageBoosters(){averageIncludeBoosters=!averageIncludeBoosters;save();}
    private boolean historyMoneyOnly = true;
    public boolean isHistoryMoneyOnly(){return historyMoneyOnly;}
    public void toggleHistoryFilter(){historyMoneyOnly=!historyMoneyOnly;save();}
    private boolean averageEnabled;
    private int averageX=12,averageY=42;
    public boolean isAverageEnabled(){return averageEnabled;}
    public void toggleAverage(){averageEnabled=!averageEnabled;BoosterStatsManager.resetAverage();save();}
    public int averageX(){return averageX;}
    public int averageY(){return averageY;}
    public void setAveragePosition(int x,int y){averageX=x;averageY=y;}
    private int offsetX = 0;
    private int offsetY = 0;
    private final Path configPath;

    private ModConfig() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("moneyboosterhud.json");
        load();
    }

    public static ModConfig getInstance() {
        return INSTANCE;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
        save();
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
    }

    public void togglePosition() {
        if (this.position == Position.TOP_CENTER) {
            this.position = Position.TOP_LEFT;
        } else {
            this.position = Position.TOP_CENTER;
        }
        save();
    }

    public void load() {
        if (!Files.exists(configPath)) {
            save();
            return;
        }
        try {
            String content = Files.readString(configPath);
            com.google.gson.JsonObject extra = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
            if(extra.has("averageIncludeBoosters"))averageIncludeBoosters=extra.get("averageIncludeBoosters").getAsBoolean();
            if(extra.has("historyMoneyOnly"))historyMoneyOnly=extra.get("historyMoneyOnly").getAsBoolean();
            if(extra.has("averageEnabled"))averageEnabled=extra.get("averageEnabled").getAsBoolean();
            if(extra.has("averageX"))averageX=extra.get("averageX").getAsInt();
            if(extra.has("averageY"))averageY=extra.get("averageY").getAsInt();
            Matcher mPos = Pattern.compile("\"position\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
            if (mPos.find()) {
                String val = mPos.group(1).trim().toUpperCase();
                if ("TOP_LEFT".equals(val)) {
                    this.position = Position.TOP_LEFT;
                } else {
                    this.position = Position.TOP_CENTER;
                }
            }
            Matcher mX = Pattern.compile("\"offsetX\"\\s*:\\s*(-?\\d+)").matcher(content);
            if (mX.find()) {
                this.offsetX = Integer.parseInt(mX.group(1));
            }
            Matcher mY = Pattern.compile("\"offsetY\"\\s*:\\s*(-?\\d+)").matcher(content);
            if (mY.find()) {
                this.offsetY = Integer.parseInt(mY.group(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            String json = "{\n" +
                    "  \"position\": \"" + this.position.name() + "\",\n" +
                    "  \"offsetX\": " + this.offsetX + ",\n" +
                    "  \"offsetY\": " + this.offsetY + ",\n" +
                    "  \"averageIncludeBoosters\": " + averageIncludeBoosters + ",\n" +
                    "  \"historyMoneyOnly\": " + historyMoneyOnly + ",\n" +
                    "  \"averageEnabled\": " + averageEnabled + ",\n" +
                    "  \"averageX\": " + averageX + ",\n" +
                    "  \"averageY\": " + averageY + "\n" +
                    "}\n";
            SafeFiles.write(configPath, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
