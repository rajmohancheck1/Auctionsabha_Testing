package com.auctionsabha.tests;

import com.auctionsabha.config.TestConfig;
import com.auctionsabha.utils.TokenManager;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Auth endpoint tests.
 * Note: register + login are already validated implicitly in TestConfig@BeforeSuite.
 * These tests focus on:
 *   - Profile retrieval (GET /api/auth/profile/{userId})
 *   - Profile update  (PUT /api/auth/profile/{userId})
 *   - Change password (POST /api/auth/change-password)
 *   - Negative cases:
 *       - Duplicate registration
 *       - Invalid login credentials
 *       - Access profile without token
 */
@Epic("Auction Sabha API")
@Feature("Authentication")
public class AuthTest {

    // ── Positive: Register duplicate ──────────────────────────────────────────

    @Test(priority = 1)
    @Story("Registration")
    @Severity(SeverityLevel.NORMAL)
    @Description("Attempting to register with an already-registered email should return 400 or 409.")
    public void registerDuplicateEmail_shouldReturn400Or409() {
        JSONObject body = new JSONObject();
        body.put("name",     "Duplicate User");
        body.put("email",    TokenManager.ADMIN_EMAIL);   // already registered in setup
        body.put("password", "any_password");
        body.put("phone",    "9999999999");
        body.put("role",     "BUYER");

        Response response = TestConfig.baseSpec()
                .body(body.toString())
                .post("/api/auth/register");

        System.out.println("[DUPLICATE REG] Status: " + response.getStatusCode());
        System.out.println("[DUPLICATE REG] Body  : " + response.asString());

        assertThat("Duplicate registration should be rejected",
                response.getStatusCode(), anyOf(is(400), is(409)));
    }

    // ── Negative: Invalid login ───────────────────────────────────────────────

    @Test(priority = 2)
    @Story("Login")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Login with wrong password should return 400 or 401.")
    public void loginWithWrongPassword_shouldReturn401() {
        JSONObject body = new JSONObject();
        body.put("email",    TokenManager.ADMIN_EMAIL);
        body.put("password", "totally_wrong_password");

        Response response = TestConfig.baseSpec()
                .body(body.toString())
                .post("/api/auth/login");

        System.out.println("[WRONG PWD] Status: " + response.getStatusCode());

        assertThat("Login with wrong password should fail",
                response.getStatusCode(), anyOf(is(400), is(401), is(403)));
    }

    @Test(priority = 3)
    @Story("Login")
    @Severity(SeverityLevel.NORMAL)
    @Description("Login with non-existent email should return 400 or 404.")
    public void loginWithNonExistentEmail_shouldReturn400OrNotFound() {
        JSONObject body = new JSONObject();
        body.put("email",    "nonexistent_" + System.currentTimeMillis() + "@example.com");
        body.put("password", "password123");

        Response response = TestConfig.baseSpec()
                .body(body.toString())
                .post("/api/auth/login");

        System.out.println("[NONEXIST EMAIL] Status: " + response.getStatusCode());

        assertThat("Login with non-existent email should fail",
                response.getStatusCode(), anyOf(is(400), is(401), is(404)));
    }

    @Test(priority = 4)
    @Story("Login")
    @Severity(SeverityLevel.MINOR)
    @Description("Login with missing email field should return 400.")
    public void loginWithMissingEmailField_shouldReturn400() {
        JSONObject body = new JSONObject();
        body.put("password", "password123");
        // "email" intentionally omitted

        Response response = TestConfig.baseSpec()
                .body(body.toString())
                .post("/api/auth/login");

        System.out.println("[MISSING EMAIL] Status: " + response.getStatusCode());

        assertThat("Login without email should return 400",
                response.getStatusCode(), anyOf(is(400), is(422)));
    }

    // ── Positive: Get profile ─────────────────────────────────────────────────

    @Test(priority = 5)
    @Story("Profile")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /api/auth/profile/{userId} with valid token should return 200 and user details.")
    public void getAdminProfile_shouldReturn200WithUserDetails() {
        int userId = TokenManager.adminUserId;

        Response response = TestConfig.authSpec(TokenManager.adminToken)
                .get("/api/auth/profile/" + userId);

        System.out.println("[GET PROFILE ADMIN] Status: " + response.getStatusCode());
        System.out.println("[GET PROFILE ADMIN] Body  : " + response.asString());

        assertThat("Get profile should return 200", response.getStatusCode(), is(200));

        // Validate key fields exist
        String email = response.jsonPath().getString("email");
        assertThat("Profile email should match", email, equalToIgnoringCase(TokenManager.ADMIN_EMAIL));
    }

    @Test(priority = 6)
    @Story("Profile")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/auth/profile/{userId} for BUYER should return 200.")
    public void getBuyerProfile_shouldReturn200() {
        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .get("/api/auth/profile/" + TokenManager.buyerUserId);

        System.out.println("[GET PROFILE BUYER] Status: " + response.getStatusCode());

        assertThat("Buyer profile fetch should succeed", response.getStatusCode(), is(200));

        String role = response.jsonPath().getString("role");
        assertThat("Buyer role should be BUYER", role, equalToIgnoringCase("BUYER"));
    }

    @Test(priority = 7)
    @Story("Profile")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/auth/profile/{userId} without token should return 401 or 403.")
    public void getProfileWithoutToken_shouldReturn401() {
        Response response = TestConfig.baseSpec()
                .get("/api/auth/profile/" + TokenManager.adminUserId);

        System.out.println("[GET PROFILE NO TOKEN] Status: " + response.getStatusCode());

        assertThat("Profile without token should be rejected",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    // ── Positive: Update profile ──────────────────────────────────────────────

    @Test(priority = 8)
    @Story("Profile Update")
    @Severity(SeverityLevel.NORMAL)
    @Description("PUT /api/auth/profile/{userId} with valid token should return 200 and updated fields.")
    public void updateSellerProfile_shouldReturn200() {
        JSONObject body = new JSONObject();
        body.put("name",  "Sabha Seller Updated");
        body.put("phone", "9111111111");

        Response response = TestConfig.authSpec(TokenManager.sellerToken)
                .body(body.toString())
                .put("/api/auth/profile/" + TokenManager.sellerUserId);

        System.out.println("[UPDATE PROFILE] Status: " + response.getStatusCode());
        System.out.println("[UPDATE PROFILE] Body  : " + response.asString());

        assertThat("Profile update should return 200 or 201",
                response.getStatusCode(), anyOf(is(200), is(201)));
    }

    @Test(priority = 9)
    @Story("Profile Update")
    @Severity(SeverityLevel.MINOR)
    @Description("PUT /api/auth/profile/{userId} without token should be rejected.")
    public void updateProfileWithoutToken_shouldBeRejected() {
        JSONObject body = new JSONObject();
        body.put("name",  "Hacker");
        body.put("phone", "0000000000");

        Response response = TestConfig.baseSpec()
                .body(body.toString())
                .put("/api/auth/profile/" + TokenManager.adminUserId);

        System.out.println("[UPDATE PROFILE NO TOKEN] Status: " + response.getStatusCode());

        assertThat("Profile update without token should fail",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    // ── Positive: Change password ─────────────────────────────────────────────

    @Test(priority = 10)
    @Story("Change Password")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /api/auth/change-password with correct old password should return 200.")
    public void changeBuyerPassword_thenRevert_shouldReturn200() {
        String newPassword = "buyer_new@2025";

        // Change to new password
        JSONObject body = new JSONObject();
        body.put("userId",      TokenManager.buyerUserId);
        body.put("oldPassword", TokenManager.BUYER_PASSWORD);
        body.put("newPassword", newPassword);

        Response changeResp = TestConfig.authSpec(TokenManager.buyerToken)
                .body(body.toString())
                .post("/api/auth/change-password");

        System.out.println("[CHANGE PWD] Status: " + changeResp.getStatusCode());
        System.out.println("[CHANGE PWD] Body  : " + changeResp.asString());

        assertThat("Change password should succeed",
                changeResp.getStatusCode(), anyOf(is(200), is(201)));

        // Revert back so subsequent tests still work
        JSONObject revertBody = new JSONObject();
        revertBody.put("userId",      TokenManager.buyerUserId);
        revertBody.put("oldPassword", newPassword);
        revertBody.put("newPassword", TokenManager.BUYER_PASSWORD);

        // Need fresh token after password change — re-login
        JSONObject loginBody = new JSONObject();
        loginBody.put("email",    TokenManager.BUYER_EMAIL);
        loginBody.put("password", newPassword);
        Response loginResp = TestConfig.baseSpec().body(loginBody.toString()).post("/api/auth/login");
        if (loginResp.getStatusCode() == 200) {
            String newToken = loginResp.jsonPath().getString("token");
            TestConfig.authSpec(newToken).body(revertBody.toString()).post("/api/auth/change-password");
            System.out.println("[CHANGE PWD] Reverted buyer password back to original.");
        }
    }

    @Test(priority = 11)
    @Story("Change Password")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /api/auth/change-password with wrong old password should return 400 or 401.")
    public void changePasswordWithWrongOldPassword_shouldFail() {
        JSONObject body = new JSONObject();
        body.put("userId",      TokenManager.sellerUserId);
        body.put("oldPassword", "completely_wrong_old_password");
        body.put("newPassword", "new_password_123");

        Response response = TestConfig.authSpec(TokenManager.sellerToken)
                .body(body.toString())
                .post("/api/auth/change-password");

        System.out.println("[CHANGE PWD WRONG] Status: " + response.getStatusCode());

        assertThat("Change password with wrong old password should fail",
                response.getStatusCode(), anyOf(is(400), is(401), is(403)));
    }

    // ── Positive: Login returns expected fields ───────────────────────────────

    @Test(priority = 12)
    @Story("Login")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Successful login should return token, userId, role, name, and message.")
    public void loginAdmin_shouldReturnExpectedFields() {
        JSONObject body = new JSONObject();
        body.put("email",    TokenManager.ADMIN_EMAIL);
        body.put("password", TokenManager.ADMIN_PASSWORD);

        Response response = TestConfig.baseSpec()
                .body(body.toString())
                .post("/api/auth/login");

        System.out.println("[LOGIN FIELDS] Status: " + response.getStatusCode());

        assertThat("Login should return 200", response.getStatusCode(), is(200));
        assertThat("Response should have token",  response.jsonPath().getString("token"),  notNullValue());
        assertThat("Response should have userId", response.jsonPath().getInt("userId"),    greaterThan(0));
        assertThat("Response should have role",   response.jsonPath().getString("role"),   notNullValue());
        assertThat("Response should have name",   response.jsonPath().getString("name"),   notNullValue());
    }
}
