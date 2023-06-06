# PatchApplier
A tool for create / apply patch, also useful in mcp-pro

# Usage
* download released version
* `java -jar PatchApplier.jar [options]`

# Options
* -make -> run tool as make patches mode
* -apply -> run tool as apply patches mode
* -dry -> only available in make mode, run make with `dry mode` default to `true`

## Example
### For make patches
`java -jar PatchApplier.jar -make /original_file /fixed_file /patch_folder -dry=true`

### For apply patches
`java -jar PatchApplier.jar -apply /original_file /patch_folder`
