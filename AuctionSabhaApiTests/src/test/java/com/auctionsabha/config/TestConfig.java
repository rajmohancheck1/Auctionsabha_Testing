package com.auctionsabha.config;

import com.auctionsabha.utils.TokenManager;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;
import org.testng.annotations.BeforeSuite;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

/**
 * Global test configuration.
 * <p>
 * @BeforeSuite:
 *   1. Configures RestAssured base URI + logging.
 *   2. Registers ADMIN, SELLER, BUYER (gracefully handles "already registered").
 *   3. Submits a verifier application for VERIFIER_APP_EMAIL.
 *   4. Logs in ADMIN and uses it to approve the verifier application.
 *   5. Logs in all roles and stores tokens + user IDs in TokenManager.
 */
public class TestConfig {

    public static final String BASE_URL = "https://auction-sabha-api.onrender.com";

    // ── Shared RequestSpecification ──────────────────────────────────────────

    /** Base spec (no auth) */
    public static RequestSpecification baseSpec() {
        return given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    /** Spec with Bearer token */
    public static RequestSpecification authSpec(String token) {
        return baseSpec().header("Authorization", "Bearer " + token);
    }

    // ── BeforeSuite ──────────────────────────────────────────────────────────

    @BeforeSuite(alwaysRun = true)
    public void globalSetup() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.baseURI = BASE_URL;
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        System.out.println("\n========================================");
        System.out.println("  Auction Sabha API — Test Suite Setup  ");
        System.out.println("========================================\n");

        // 1. Register users (idempotent — 400 means already exists → just login)
        registerUser(TokenManager.ADMIN_EMAIL,    TokenManager.ADMIN_PASSWORD,    "Admin User",    "9000000001", "ADMIN");
        registerUser(TokenManager.SELLER_EMAIL,   TokenManager.SELLER_PASSWORD,   "Sabha Seller",  "9000000002", "SELLER");
        registerUser(TokenManager.BUYER_EMAIL,    TokenManager.BUYER_PASSWORD,    "Sabha Buyer",   "9000000003", "BUYER");

        // 2. Login all three and save tokens
        loginAndSave(TokenManager.ADMIN_EMAIL,  TokenManager.ADMIN_PASSWORD,  "ADMIN");
        loginAndSave(TokenManager.SELLER_EMAIL, TokenManager.SELLER_PASSWORD, "SELLER");
        loginAndSave(TokenManager.BUYER_EMAIL,  TokenManager.BUYER_PASSWORD,  "BUYER");

        // 3. Submit verifier application + approve as ADMIN + login as VERIFIER
        setupVerifier();

        System.out.println("\n── Tokens stored ─────────────────────────────");
        System.out.println("ADMIN    userId=" + TokenManager.adminUserId);
        System.out.println("SELLER   userId=" + TokenManager.sellerUserId);
        System.out.println("BUYER    userId=" + TokenManager.buyerUserId);
        System.out.println("VERIFIER userId=" + TokenManager.verifierUserId);
        System.out.println("──────────────────────────────────────────────\n");
    }

    // ── Private Helpers ──────────────────────────────────────────────────────
     // ── Private Helpers ──────────────────────────────────────────────────────
    

    @Step("Register user: {email} as {role}")
    private void registerUser(String email, String password, String name, String phone, String role) {
        JSONObject body = new JSONObject();
        body.put("name",     name);
        body.put("email",    email);
        body.put("password", password);
        body.put("phone",    phone);
        body.put("role",     role);

        Response response = baseSpec()
                .body(body.toString())
                .post("/api/auth/register");

        int status = response.getStatusCode();
        if (status == 201 || status == 200) {
            System.out.println("[REGISTER] " + role + " created: " + email);
        } else if (status == 400) {
            // API may return plain text or JSON for duplicate-email errors
            String respBody = response.asString();
            if (respBody != null && (respBody.toLowerCase().contains("already") || respBody.toLowerCase().contains("exists"))) {
                System.out.println("[REGISTER] " + role + " already exists — will login: " + email);
            } else {
                System.out.println("[REGISTER] Unexpected 400 for " + email + " — " + respBody);
            }
        } else if (status == 409) {
            System.out.println("[REGISTER] " + role + " conflict (409) — already registered: " + email);
        } else {
            System.out.println("[REGISTER] " + role + " unexpected status " + status + " — " + response.asString());
        }
    }

    @Step("Login and save token for: {email} ({role})")
    private void loginAndSave(String email, String password, String role) {
        JSONObject body = new JSONObject();
        body.put("email",    email);
        body.put("password", password);

        Response response = baseSpec()
                .body(body.toString())
                .post("/api/auth/login");

        if (response.getStatusCode() != 200) {
            throw new RuntimeException("[LOGIN FAILED] " + role + " (" + email + ") → "
                    + response.getStatusCode() + " " + response.asString());
        }

        String token  = response.jsonPath().getString("token");
        int    userId = response.jsonPath().getInt("userId");

        switch (role) {
            case "ADMIN":
                TokenManager.adminToken  = token;
                TokenManager.adminUserId = userId;
                break;
            case "SELLER":
                TokenManager.sellerToken  = token;
                TokenManager.sellerUserId = userId;
                break;
            case "BUYER":
                TokenManager.buyerToken  = token;
                TokenManager.buyerUserId = userId;
                break;
            case "VERIFIER":
                TokenManager.verifierToken  = token;
                TokenManager.verifierUserId = userId;
                break;
            default:
                throw new IllegalArgumentException("Unknown role: " + role);
        }
        System.out.println("[LOGIN] " + role + " (userId=" + userId + ") token saved.");
    }

    @Step("Setup VERIFIER: submit application → admin approves → login")
    private void setupVerifier() {
        // Step A: Submit verifier application (public endpoint)
        JSONObject appBody = new JSONObject();
        appBody.put("name",          TokenManager.VERIFIER_APP_NAME);
        appBody.put("email",         TokenManager.VERIFIER_APP_EMAIL);
        appBody.put("phone",         "9000000004");
        appBody.put("qualification", "Certified Appraiser");
        appBody.put("experience",    "5 years in auction house valuation");
        appBody.put("documents",     "https://example.com/docs/verifier_cert.pdf");

        Response appResponse = baseSpec()
                .body(appBody.toString())
                .post("/api/verifier-applications");

        int appStatus = appResponse.getStatusCode();
        String appRespBody = appResponse.asString();
        System.out.println("[VERIFIER APP] Submit status: " + appStatus);

        if (appStatus == 200 || appStatus == 201) {
            // Response may be plain text like "Application submitted successfully! ID: 2"
            // or JSON — handle both
            Matcher m = Pattern.compile("ID:\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(appRespBody);
            if (m.find()) {
                TokenManager.verifierAppId = Integer.parseInt(m.group(1));
                System.out.println("[VERIFIER APP] Created with id=" + TokenManager.verifierAppId);
            } else if (appRespBody.trim().startsWith("{")) {
                // Try JSON fields as fallback
                try {
                    Object idObj = appResponse.jsonPath().get("id");
                    if (idObj == null) idObj = appResponse.jsonPath().get("applicationId");
                    if (idObj != null) {
                        TokenManager.verifierAppId = Integer.parseInt(idObj.toString());
                        System.out.println("[VERIFIER APP] Created with id=" + TokenManager.verifierAppId);
                    }
                } catch (Exception ignored) {
                    System.out.println("[VERIFIER APP] Could not parse ID from JSON response: " + appRespBody);
                }
            } else {
                System.out.println("[VERIFIER APP] Could not extract ID from response: " + appRespBody);
            }
        } else if (appStatus == 400 || appStatus == 409) {
            System.out.println("[VERIFIER APP] Application likely already submitted — fetching pending list.");
        } else {
            System.out.println("[VERIFIER APP] Unexpected status " + appStatus + " → " + appResponse.asString());
        }

        // Step B: If we don't have the app ID, try fetching the pending list as ADMIN
        if (TokenManager.verifierAppId == 0 && !TokenManager.adminToken.isEmpty()) {
            Response pendingResp = authSpec(TokenManager.adminToken)
                    .get("/api/verifier-applications/pending");
            if (pendingResp.getStatusCode() == 200) {
                // Find app by email
                String idFromList = pendingResp.jsonPath()
                        .getString("find { it.email == '" + TokenManager.VERIFIER_APP_EMAIL + "' }.id");
                if (idFromList != null && !idFromList.isEmpty()) {
                    TokenManager.verifierAppId = Integer.parseInt(idFromList);
                    System.out.println("[VERIFIER APP] Found in pending list: id=" + TokenManager.verifierAppId);
                } else {
                    // Maybe already approved — look in all applications
                    Response allApps = authSpec(TokenManager.adminToken)
                            .get("/api/verifier-applications");
                    if (allApps.getStatusCode() == 200) {
                        String idFromAll = allApps.jsonPath()
                                .getString("find { it.email == '" + TokenManager.VERIFIER_APP_EMAIL + "' }.id");
                        if (idFromAll != null && !idFromAll.isEmpty()) {
                            TokenManager.verifierAppId = Integer.parseInt(idFromAll);
                            System.out.println("[VERIFIER APP] Found in all-apps list: id=" + TokenManager.verifierAppId);
                        }
                    }
                }
            }
        }

        // Step C: Approve the application as ADMIN
        if (TokenManager.verifierAppId > 0 && !TokenManager.adminToken.isEmpty()) {
            Response approveResp = authSpec(TokenManager.adminToken)
                    .queryParam("remarks", "Approved by automated test suite")
                    .put("/api/verifier-applications/" + TokenManager.verifierAppId + "/approve");

            int approveStatus = approveResp.getStatusCode();
            System.out.println("[VERIFIER APPROVE] Status: " + approveStatus + " → " + approveResp.asString());

            if (approveStatus == 200 || approveStatus == 201) {
                // API returns a VerifierApprovalResponse JSON with tempPassword field
                String approveBody = approveResp.asString();
                String tempPassword = null;
                if (approveBody.trim().startsWith("{")) {
                    try {
                        tempPassword = approveResp.jsonPath().getString("tempPassword");
                        if (tempPassword == null) tempPassword = approveResp.jsonPath().getString("password");
                        if (tempPassword == null) tempPassword = approveResp.jsonPath().getString("generatedPassword");
                        if (tempPassword == null) tempPassword = approveResp.jsonPath().getString("data.tempPassword");
                    } catch (Exception e) {
                        System.out.println("[VERIFIER APPROVE] Could not parse approve JSON: " + approveBody);
                    }
                }

                if (tempPassword != null && !tempPassword.isEmpty()) {
                    TokenManager.verifierPassword = tempPassword;
                    TokenManager.verifierEmail    = TokenManager.VERIFIER_APP_EMAIL;
                    System.out.println("[VERIFIER APPROVE] Temp password received.");
                    loginAndSave(TokenManager.verifierEmail, TokenManager.verifierPassword, "VERIFIER");
                } else {
                    System.out.println("[VERIFIER APPROVE] No temp password in response; trying fallback logins.");
                    tryVerifierLogin();
                }
            } else if (approveStatus == 400) {
                // Already approved — try logging in with a previously known password
                System.out.println("[VERIFIER APPROVE] Might already be approved. Trying login with known credentials.");
                tryVerifierLogin();
            }
        } else {
            System.out.println("[VERIFIER APP] No app ID found — VERIFIER tests will be skipped/limited.");
            tryVerifierLogin();
        }
    }

    /** Attempt verifier login with any previously-used password (fallback). */
    private void tryVerifierLogin() {
        // If the server persists data and we approved on a previous run, try "Verifier@2025"
        String[] fallbacks = { "Verifier@2025", "verifier@2025", "Welcome@1" };
        for (String pwd : fallbacks) {
            JSONObject body = new JSONObject();
            body.put("email",    TokenManager.VERIFIER_APP_EMAIL);
            body.put("password", pwd);

            Response resp = baseSpec().body(body.toString()).post("/api/auth/login");
            if (resp.getStatusCode() == 200) {
                TokenManager.verifierToken  = resp.jsonPath().getString("token");
                TokenManager.verifierUserId = resp.jsonPath().getInt("userId");
                TokenManager.verifierEmail  = TokenManager.VERIFIER_APP_EMAIL;
                TokenManager.verifierPassword = pwd;
                System.out.println("[VERIFIER LOGIN] Fallback login succeeded with pwd=" + pwd
                        + " userId=" + TokenManager.verifierUserId);
                return;
            }
        }
        System.out.println("[VERIFIER LOGIN] All fallback attempts failed. Verifier token stays empty.");
    }
}
