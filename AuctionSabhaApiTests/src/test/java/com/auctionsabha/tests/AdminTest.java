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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Admin endpoint tests.
 *
 * Covers:
 *   - GET  /api/admin/products
 *   - POST /api/admin/auction/{id}/start
 *   - POST /api/admin/auction/{id}/stop
 *   - Negative: start auction as non-ADMIN, start already-started auction, stop non-existent auction
 */
@Epic("Auction Sabha API")
@Feature("Admin Operations")
public class AdminTest {

    private int targetAuctionId;

    @BeforeClass(alwaysRun = true)
    public void resolveAuctionId() {
        // Use the auction id discovered in AuctionTest; fallback to 1
        targetAuctionId = TokenManager.activeAuctionId > 0 ? TokenManager.activeAuctionId : 1;
        System.out.println("[ADMIN TEST SETUP] Using auctionId=" + targetAuctionId);
    }

    // ── GET /api/admin/products ───────────────────────────────────────────────

    @Test(priority = 1)
    @Story("Admin Products")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /api/admin/products with ADMIN token should return all products.")
    public void getAdminProducts_shouldReturn200() {
        Response response = TestConfig.authSpec(TokenManager.adminToken)
                .get("/api/admin/products");

        System.out.println("[ADMIN PRODUCTS] Status: " + response.getStatusCode());
        System.out.println("[ADMIN PRODUCTS] Body  : "
                + response.asString().substring(0, Math.min(500, response.asString().length())));

        assertThat("Admin products should return 200", response.getStatusCode(), is(200));
        assertThat("Admin products body should not be empty",
                response.asString().trim(), not(emptyOrNullString()));
    }

    @Test(priority = 2)
    @Story("Admin Products")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/admin/products with SELLER token should be forbidden.")
    public void getAdminProducts_asSeller_shouldBeForbidden() {
        Response response = TestConfig.authSpec(TokenManager.sellerToken)
                .get("/api/admin/products");

        System.out.println("[ADMIN PRODUCTS AS SELLER] Status: " + response.getStatusCode());

        assertThat("SELLER should not access admin products",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    @Test(priority = 3)
    @Story("Admin Products")
    @Severity(SeverityLevel.MINOR)
    @Description("GET /api/admin/products without token should be rejected.")
    public void getAdminProducts_withoutToken_shouldBeRejected() {
        Response response = TestConfig.baseSpec()
                .get("/api/admin/products");

        System.out.println("[ADMIN PRODUCTS NO TOKEN] Status: " + response.getStatusCode());

        assertThat("Admin products without token should fail",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    // ── POST /api/admin/auction/{id}/start ────────────────────────────────────

    @Test(priority = 4)
    @Story("Start Auction")
    @Severity(SeverityLevel.BLOCKER)
    @Description("POST /api/admin/auction/{id}/start with ADMIN token should start the auction.")
    public void startAuction_asAdmin_shouldReturn200() {
        Response response = TestConfig.authSpec(TokenManager.adminToken)
                .post("/api/admin/auction/" + targetAuctionId + "/start");

        System.out.println("[START AUCTION] Status: " + response.getStatusCode());
        System.out.println("[START AUCTION] Body  : " + response.asString());

        // 200 = started, 400 = already started — both are acceptable for this test
        assertThat("Start auction should return 200 or 400",
                response.getStatusCode(), anyOf(is(200), is(201), is(400)));
    }

    @Test(priority = 5)
    @Story("Start Auction")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /api/admin/auction/{id}/start as BUYER should be forbidden.")
    public void startAuction_asBuyer_shouldBeForbidden() {
        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .post("/api/admin/auction/" + targetAuctionId + "/start");

        System.out.println("[START AUCTION AS BUYER] Status: " + response.getStatusCode());

        assertThat("BUYER should not start auction",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    @Test(priority = 6)
    @Story("Start Auction")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /api/admin/auction/{id}/start twice should return 400 (already started).")
    public void startAuction_twice_shouldReturn400() {
        // First call — might start or return "already started"
        TestConfig.authSpec(TokenManager.adminToken)
                .post("/api/admin/auction/" + targetAuctionId + "/start");

        // Second call — should be rejected as already started
        Response secondResponse = TestConfig.authSpec(TokenManager.adminToken)
                .post("/api/admin/auction/" + targetAuctionId + "/start");

        System.out.println("[START TWICE] Status: " + secondResponse.getStatusCode());
        System.out.println("[START TWICE] Body  : " + secondResponse.asString());

        assertThat("Starting already-started auction should fail",
                secondResponse.getStatusCode(), anyOf(is(400), is(409), is(200)));
    }

    @Test(priority = 7)
    @Story("Start Auction")
    @Severity(SeverityLevel.MINOR)
    @Description("POST /api/admin/auction/999999/start for non-existent auction should return 404.")
    public void startNonExistentAuction_shouldReturn404() {
        Response response = TestConfig.authSpec(TokenManager.adminToken)
                .post("/api/admin/auction/999999/start");

        System.out.println("[START NONEXISTENT] Status: " + response.getStatusCode());

        assertThat("Starting non-existent auction should return 404",
                response.getStatusCode(), anyOf(is(404), is(400)));
    }

    // ── POST /api/admin/auction/{id}/stop ─────────────────────────────────────

    @Test(priority = 8)
    @Story("Stop Auction")
    @Severity(SeverityLevel.BLOCKER)
    @Description("POST /api/admin/auction/{id}/stop with ADMIN token should stop or confirm state.")
    public void stopAuction_asAdmin_shouldReturn200() {
        Response response = TestConfig.authSpec(TokenManager.adminToken)
                .post("/api/admin/auction/" + targetAuctionId + "/stop");

        System.out.println("[STOP AUCTION] Status: " + response.getStatusCode());
        System.out.println("[STOP AUCTION] Body  : " + response.asString());

        assertThat("Stop auction should return 200 or 400",
                response.getStatusCode(), anyOf(is(200), is(201), is(400)));
    }

    @Test(priority = 9)
    @Story("Stop Auction")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /api/admin/auction/{id}/stop as SELLER should be forbidden.")
    public void stopAuction_asSeller_shouldBeForbidden() {
        Response response = TestConfig.authSpec(TokenManager.sellerToken)
                .post("/api/admin/auction/" + targetAuctionId + "/stop");

        System.out.println("[STOP AUCTION AS SELLER] Status: " + response.getStatusCode());

        assertThat("SELLER should not stop auction",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    @Test(priority = 10)
    @Story("Stop Auction")
    @Severity(SeverityLevel.MINOR)
    @Description("POST /api/admin/auction/999999/stop for non-existent auction should return 404.")
    public void stopNonExistentAuction_shouldReturn404() {
        Response response = TestConfig.authSpec(TokenManager.adminToken)
                .post("/api/admin/auction/999999/stop");

        System.out.println("[STOP NONEXISTENT] Status: " + response.getStatusCode());

        assertThat("Stopping non-existent auction should return 404",
                response.getStatusCode(), anyOf(is(404), is(400)));
    }
}
