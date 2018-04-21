package com.github.japanoise;

import java.nio.file.Path;

class DoomGameFile {
    private String shortName;
    private Path filepath;

    DoomGameFile(Path filepath) {
        this.filepath = filepath;
        shortName = filepath.getFileName().toString();
    }

    public Path getFilepath() {
        return filepath;
    }

    public String getShortName() {
        return shortName;
    }

    @Override
    public String toString() {
        return getShortName();
    }
}
