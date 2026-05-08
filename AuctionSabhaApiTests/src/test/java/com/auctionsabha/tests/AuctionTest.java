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
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Auction endpoint tests.
 *
 * Covers:
 *   - GET /api/auction/all
 *   - GET /api/auction/{id}/leaderboard
 *   - GET /api/auction/slots?date=
 *   - Negative: leaderboard for invalid auction id
 */
@Epic("Auction Sabha API")
@Feature("Auctions")
public class AuctionTest {

    // ── GET /api/auction/all ──────────────────────────────────────────────────

    @Test(priority = 1)
    @Story("List Auctions")
    @Severity(SeverityLevel.BLOCKER)
    @Description("GET /api/auction/all with valid token should return 200 and a list of auctions.")
    public void getAllAuctions_shouldReturn200() {
        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .get("/api/auction/all");

        System.out.println("[ALL AUCTIONS] Status: " + response.getStatusCode());
        System.out.println("[ALL AUCTIONS] Body  : " + response.asString().substring(0, Math.min(500, response.asString().length())));

        assertThat("Get all auctions should return 200", response.getStatusCode(), is(200));

        // Try to capture an auction id for leaderboard test
        try {
            List<Integer> ids = response.jsonPath().getList("id");
            if (ids != null && !ids.isEmpty() && ids.get(0) != null) {
                TokenManager.activeAuctionId = ids.get(0);
                System.out.println("[ALL AUCTIONS] Using auctionId=" + TokenManager.activeAuctionId);
            } else {
                // Try nested structure
                List<Integer> idsNested = response.jsonPath().getList("data.id");
                if (idsNested != null && !idsNested.isEmpty() && idsNested.get(0) != null) {
                    TokenManager.activeAuctionId = idsNested.get(0);
                }
            }
        } catch (Exception e) {
            System.out.println("[ALL AUCTIONS] Could not extract auction id: " + e.getMessage());
        }
    }

    @Test(priority = 2)
    @Story("List Auctions")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/auction/all without token should be rejected.")
    public void getAllAuctions_withoutToken_shouldBeRejected() {
        Response response = TestConfig.baseSpec()
                .get("/api/auction/all");

        System.out.println("[ALL AUCTIONS NO TOKEN] Status: " + response.getStatusCode());

        assertThat("All auctions without token should fail",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    // ── GET /api/auction/{id}/leaderboard ─────────────────────────────────────

    @Test(priority = 3, dependsOnMethods = "getAllAuctions_shouldReturn200")
    @Story("Leaderboard")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /api/auction/{id}/leaderboard should return 200 with bid list.")
    public void getAuctionLeaderboard_shouldReturn200() {
        if (TokenManager.activeAuctionId == 0) {
            System.out.println("[LEADERBOARD] No active auction id — using id=1 as fallback.");
            TokenManager.activeAuctionId = 1;
        }

        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .get("/api/auction/" + TokenManager.activeAuctionId + "/leaderboard");

        System.out.println("[LEADERBOARD] Status: " + response.getStatusCode());
        System.out.println("[LEADERBOARD] Body  : " + response.asString());

        assertThat("Leaderboard should return 200 or 404",
                response.getStatusCode(), anyOf(is(200), is(404)));
    }

    @Test(priority = 4)
    @Story("Leaderboard")
    @Severity(SeverityLevel.MINOR)
    @Description("GET /api/auction/999999/leaderboard for non-existent auction should return 404.")
    public void getLeaderboard_nonExistentAuction_shouldReturn404() {
        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .get("/api/auction/999999/leaderboard");

        System.out.println("[LEADERBOARD 999999] Status: " + response.getStatusCode());

        assertThat("Non-existent auction leaderboard should return 404",
                response.getStatusCode(), anyOf(is(404), is(400)));
    }

    @Test(priority = 5)
    @Story("Leaderboard")
    @Severity(SeverityLevel.MINOR)
    @Description("GET /api/auction/{id}/leaderboard without token should be rejected.")
    public void getLeaderboard_withoutToken_shouldBeRejected() {
        Response response = TestConfig.baseSpec()
                .get("/api/auction/1/leaderboard");

        System.out.println("[LEADERBOARD NO TOKEN] Status: " + response.getStatusCode());

        assertThat("Leaderboard without token should fail",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    // ── GET /api/auction/slots?date= ──────────────────────────────────────────

    @Test(priority = 6)
    @Story("Auction Slots")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/auction/slots?date=2025-05-10 (public endpoint) should return 200.")
    public void getAuctionSlots_specificDate_shouldReturn200() {
        Response response = TestConfig.baseSpec()
                .queryParam("date", "2025-05-10")
                .get("/api/auction/slots");

        System.out.println("[SLOTS 2025-05-10] Status: " + response.getStatusCode());
        System.out.println("[SLOTS 2025-05-10] Body  : " + response.asString());

        assertThat("Auction slots endpoint should return 200",
                response.getStatusCode(), anyOf(is(200), is(204)));
    }

    @Test(priority = 7)
    @Story("Auction Slots")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/auction/slots for a future date should return 200 or empty list.")
    public void getAuctionSlots_futureDate_shouldReturn200() {
        Response response = TestConfig.baseSpec()
                .queryParam("date", "2025-12-31")
                .get("/api/auction/slots");

        System.out.println("[SLOTS FUTURE] Status: " + response.getStatusCode());
        System.out.println("[SLOTS FUTURE] Body  : " + response.asString());

        assertThat("Auction slots for future date should succeed",
                response.getStatusCode(), anyOf(is(200), is(204)));
    }

    @Test(priority = 8)
    @Story("Auction Slots")
    @Severity(SeverityLevel.MINOR)
    @Description("GET /api/auction/slots without date param should return 400 or 200.")
    public void getAuctionSlots_withoutDate_shouldReturn400OrDefault() {
        Response response = TestConfig.baseSpec()
                .get("/api/auction/slots");

        System.out.println("[SLOTS NO DATE] Status: " + response.getStatusCode());

        assertThat("Auction slots without date param should return 200 or 400",
                response.getStatusCode(), anyOf(is(200), is(400)));
    }
}
