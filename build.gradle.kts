plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// Not a git checkout, so the version cannot come from a tag (gtnh.modules.gitVersion=false).
project.ext.set("modVersion", "1.0.0")
version = "1.0.0"
