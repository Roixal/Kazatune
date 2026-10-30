package com.kazatune.updater.util

class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: String? = null,
    val rawString: String
) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int {
        if (this.major != other.major) {
            return this.major.compareTo(other.major)
        }
        if (this.minor != other.minor) {
            return this.minor.compareTo(other.minor)
        }
        if (this.patch != other.patch) {
            return this.patch.compareTo(other.patch)
        }
        // If versions are equal, a release without prerelease suffix is considered higher than a prerelease
        // e.g., 1.0.1 > 1.0.1-beta
        return when {
            this.prerelease == null && other.prerelease == null -> 0
            this.prerelease == null && other.prerelease != null -> 1
            this.prerelease != null && other.prerelease == null -> -1
            else -> (this.prerelease ?: "").compareTo(other.prerelease ?: "")
        }
    }

    fun isNewerThan(other: SemanticVersion): Boolean = this > other

    override fun toString(): String {
        val base = "$major.$minor.$patch"
        return if (prerelease != null) "$base-$prerelease" else base
    }

    companion object {
        private val VERSION_REGEX = Regex("""(?:v|V)?(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:[-.]([0-9A-Za-z.-]+))?""")

        fun parse(input: String?): SemanticVersion? {
            if (input.isNullOrBlank()) return null
            val match = VERSION_REGEX.find(input.trim()) ?: return null
            val major = match.groups[1]?.value?.toIntOrNull() ?: 0
            val minor = match.groups[2]?.value?.toIntOrNull() ?: 0
            val patch = match.groups[3]?.value?.toIntOrNull() ?: 0
            val prerelease = match.groups[4]?.value?.takeIf { it.isNotBlank() }

            return SemanticVersion(
                major = major,
                minor = minor,
                patch = patch,
                prerelease = prerelease,
                rawString = input.trim()
            )
        }
    }
}
