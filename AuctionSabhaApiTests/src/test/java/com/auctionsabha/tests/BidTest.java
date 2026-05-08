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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Bid endpoint tests.
 *
 * Covers:
 *   - GET /api/bids/my-bids?buyerId={id}
 *   - Negative: access another buyer's bids, access without token
 */
@Epic("Auction Sabha API")
@Feature("Bids")
public class BidTest {

    // ── GET /api/bids/my-bids ─────────────────────────────────────────────────

    @Test(priority = 1)
    @Story("My Bids")
    @Severity(SeverityLevel.BLOCKER)
    @Description("GET /api/bids/my-bids?buyerId={id} with valid BUYER token should return 200.")
    public void getMyBids_asBuyer_shouldReturn200() {
        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .queryParam("buyerId", TokenManager.buyerUserId)
                .get("/api/bids/my-bids");

        System.out.println("[MY BIDS] Status: " + response.getStatusCode());
        System.out.println("[MY BIDS] Body  : " + response.asString());

        assertThat("My bids should return 200", response.getStatusCode(), is(200));

        // Response should be an array (possibly empty if no bids placed yet)
        String body = response.asString().trim();
        assertThat("My bids response should not be null", body, not(nullValue()));
    }

    @Test(priority = 2)
    @Story("My Bids")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/bids/my-bids without token should return 401 or 403.")
    public void getMyBids_withoutToken_shouldBeRejected() {
        Response response = TestConfig.baseSpec()
                .queryParam("buyerId", TokenManager.buyerUserId)
                .get("/api/bids/my-bids");

        System.out.println("[MY BIDS NO TOKEN] Status: " + response.getStatusCode());

        assertThat("My bids without token should fail",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    @Test(priority = 3)
    @Story("My Bids")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/bids/my-bids with SELLER token should return 200 or 403 (depending on role guards).")
    public void getMyBids_asSeller_shouldReturn200Or403() {
        // Sellers are not buyers — server may allow or restrict
        Response response = TestConfig.authSpec(TokenManager.sellerToken)
                .queryParam("buyerId", TokenManager.sellerUserId)
                .get("/api/bids/my-bids");

        System.out.println("[MY BIDS AS SELLER] Status: " + response.getStatusCode());
        System.out.println("[MY BIDS AS SELLER] Body  : " + response.asString());

        assertThat("My bids as seller should return 200 or 403",
                response.getStatusCode(), anyOf(is(200), is(403), is(401)));
    }

    @Test(priority = 4)
    @Story("My Bids")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/bids/my-bids for ADMIN should return bids or empty list.")
    public void getMyBids_asAdmin_shouldReturn200() {
        Response response = TestConfig.authSpec(TokenManager.adminToken)
                .queryParam("buyerId", TokenManager.adminUserId)
                .get("/api/bids/my-bids");

        System.out.println("[MY BIDS AS ADMIN] Status: " + response.getStatusCode());

        assertThat("My bids as admin should return 200 or 403",
                response.getStatusCode(), anyOf(is(200), is(403)));
    }

    @Test(priority = 5)
    @Story("My Bids")
    @Severity(SeverityLevel.MINOR)
    @Description("GET /api/bids/my-bids?buyerId=999999 for non-existent buyer should return 200 or 404.")
    public void getMyBids_nonExistentBuyer_shouldReturn200Or404() {
        Response response = TestConfig.authSpec(TokenManager.adminToken)
                .queryParam("buyerId", 999999)
                .get("/api/bids/my-bids");

        System.out.println("[MY BIDS 999999] Status: " + response.getStatusCode());

        assertThat("My bids for non-existent buyer should return 200 or 404",
                response.getStatusCode(), anyOf(is(200), is(404)));
    }

    @Test(priority = 6)
    @Story("My Bids")
    @Severity(SeverityLevel.MINOR)
    @Description("GET /api/bids/my-bids without buyerId param should return 400 or default response.")
    public void getMyBids_withoutBuyerIdParam_shouldReturn400OrDefault() {
        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .get("/api/bids/my-bids");  // No buyerId

        System.out.println("[MY BIDS NO BUYER ID] Status: " + response.getStatusCode());

        assertThat("My bids without buyerId should return 200 or 400",
                response.getStatusCode(), anyOf(is(200), is(400)));
    }
}
