package com.auctionsabha.utils;

/**
 * Shared static store for tokens and user IDs.
 * Populated once in TestConfig#@BeforeSuite and read by all test classes.
 */
public class TokenManager {

    // ── Tokens ──────────────────────────────────────────────────────────────
    public static volatile String adminToken    = "";
    public static volatile String sellerToken   = "";
    public static volatile String buyerToken    = "";
    public static volatile String verifierToken = "";

    // ── User IDs ─────────────────────────────────────────────────────────────
    public static volatile int adminUserId    = 0;
    public static volatile int sellerUserId   = 0;
    public static volatile int buyerUserId    = 0;
    public static volatile int verifierUserId = 0;

    // ── Credentials (used in change-password tests) ──────────────────────────
    public static final String ADMIN_EMAIL    = "admin_sabha@example.com";
    public static final String ADMIN_PASSWORD = "admin@2025";

    public static final String SELLER_EMAIL    = "seller_sabha@example.com";
    public static final String SELLER_PASSWORD = "seller@2025";

    public static final String BUYER_EMAIL    = "buyer_sabha@example.com";
    public static final String BUYER_PASSWORD = "buyer@2025";

    // Verifier credentials are set dynamically after approval
    public static volatile String verifierEmail    = "";
    public static volatile String verifierPassword = "";

    // ── Verifier application applicant details ────────────────────────────────
    public static final String VERIFIER_APP_EMAIL = "verifier_sabha@example.com";
    public static final String VERIFIER_APP_NAME  = "Sabha Verifier";

    // ── Product / Auction IDs set during product tests ────────────────────────
    public static volatile int submittedProductId  = 0;
    public static volatile int approvedProductId   = 0;
    public static volatile int activeAuctionId     = 0;
    public static volatile int verifierAppId       = 0;

    private TokenManager() { /* utility class */ }
}
