package com.github.japanoise.jdoomlauncher;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigContainer implements Serializable {
    List<String> pwadPaths;
    List<String> iwadPaths;
    List<String> sourceports;

    public ConfigContainer(List<DoomGameFile> sourceports, List<Path> pwads, List<Path> iwads) {
        this.sourceports = sourceports.stream().map(doomGameFile -> doomGameFile.getFilepath().toString()).collect(Collectors.toList());
        this.iwadPaths = iwads.stream().map(Path::toString).collect(Collectors.toList());
        this.pwadPaths = pwads.stream().map(Path::toString).collect(Collectors.toList());
    }
}
