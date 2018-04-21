# jdoomlauncher

jdoomlauncher is a simple Doom launcher.

## Building

1. import this project into intellij
2. build artifacts
3. find the .jar in the out/artifacts directory

## Files

jdoomlauncher creates $XDG_CONFIG_HOME/jdoomlauncher, and saves its data to
config.bin in this directory.

jdoomlauncher adheres to xdg standards; it will default to ~/.config if
$XDG_CONFIG_HOME is unset.

## License

MIT.
