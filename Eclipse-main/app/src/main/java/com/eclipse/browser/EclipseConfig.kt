package com.eclipse.browser

/**
 * EclipseConfig — Single source of truth for all backend URLs and app constants.
 *
 * Section 31.2: This is the ONLY place the backend URL exists in the entire app.
 * Never hardcode any URL anywhere else. Always reference these constants.
 *
 * Section 31.16: No API keys stored here or anywhere in the app.
 * All sensitive operations happen on the Cloudflare Workers backend.
 */
object EclipseConfig {

    // ── CLOUDFLARE WORKERS BACKEND ──
    // Section 39.1: Live and running
    const val BACKEND_URL       = "https://eclipse-ai-backend.creedgameradi.workers.dev"

    // ── BACKEND ENDPOINTS ──
    // Section 31.2: All endpoint constants defined here
    const val ENDPOINT_CHAT      = "$BACKEND_URL/chat"
    const val ENDPOINT_VISION    = "$BACKEND_URL/vision"
    const val ENDPOINT_FILES     = "$BACKEND_URL/files"
    const val ENDPOINT_GENERATE  = "$BACKEND_URL/generate"
    const val ENDPOINT_TRANSFORM = "$BACKEND_URL/transform"
    const val ENDPOINT_SEARCH    = "$BACKEND_URL/search"
    const val ENDPOINT_HEALTH    = "$BACKEND_URL/health"

    // ── EXTENSIONS — GREASY FORK ──
    // Section 29: Greasy Fork public API — no key needed
    const val GREASYFORK_API     = "https://greasyfork.org/scripts.json"
    const val GREASYFORK_PER_PAGE = 20

    // ── AI CONFIG ──
    // Section 8: Response mode values sent to backend
    const val MODE_SHORT    = "short"
    const val MODE_DETAILED = "detailed"
    const val MODE_PRECISE  = "precise"

    // Section 31.12: Thresholds for remaining message warnings
    const val WARN_MESSAGES_THRESHOLD  = 50   // show subtle warning below this
    const val DANGER_MESSAGES_THRESHOLD = 20   // show stronger warning below this

    // ── IMAGE GENERATION KEYWORDS ──
    // Section 31.11: Detect image generation intent
    val IMAGE_GEN_KEYWORDS = listOf(
        "generate an image",
        "create an image",
        "draw",
        "make an image",
        "generate a picture",
        "create a picture",
        "show me an image of",
        "generate art",
        "create art",
        "paint",
        "illustrate"
    )

    // ── SUPABASE ──
    // Section 37: Database credentials
    // NOTE: Using publishable (anon) key only — safe to include per Supabase docs when RLS is enabled
    const val SUPABASE_URL      = "https://hjdaxklnvwehiehpolpv.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_hx_35e5OiVP5obhWdazlkA_iPcg_4zT"
    // Replace SUPABASE_ANON_KEY with your actual publishable key from Supabase dashboard

    // ── BACKGROUND THEMES ──
    // Section 14: Pre-loaded background drawable names
    val BACKGROUND_THEMES = listOf(
        "eclipse",    // bg_eclipse  — planet with fire
        "nebula",     // bg_nebula   — solar system blue
        "aurora",     // bg_aurora   — planets above clouds
        "milkyway",   // bg_milkyway — Milky Way
        "blackhole",  // bg_blackhole — black hole
        "cosmos",     // bg_cosmos   — colorful stars
        "galaxy"      // bg_galaxy   — spiral galaxy
    )
}
