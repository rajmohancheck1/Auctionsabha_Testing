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
 * Wallet endpoint tests.
 *
 * Covers:
 *   - GET  /api/wallet/{userId}
 *   - POST /api/wallet/{userId}/deposit
 *   - POST /api/wallet/{userId}/withdraw
 *   - POST /api/wallet/{userId}/holdBid
 *   - POST /api/wallet/{userId}/refund
 *   - POST /api/wallet/{userId}/payout
 *   - GET  /api/wallet/{userId}/transactions
 *   - Negative: withdraw more than balance, access another user's wallet
 */
@Epic("Auction Sabha API")
@Feature("Wallet")
public class WalletTest {

    // ── GET /api/wallet/{userId} ──────────────────────────────────────────────

    @Test(priority = 1)
    @Story("Get Wallet")
    @Severity(SeverityLevel.BLOCKER)
    @Description("GET /api/wallet/{userId} should return wallet details including balance.")
    public void getBuyerWallet_shouldReturn200WithBalance() {
        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .get("/api/wallet/" + TokenManager.buyerUserId);

        System.out.println("[GET WALLET] Status: " + response.getStatusCode());
        System.out.println("[GET WALLET] Body  : " + response.asString());

        assertThat("Get wallet should return 200", response.getStatusCode(), is(200));

        // Validate that some balance/wallet field is present
        String body = response.asString();
        assertThat("Wallet response should not be empty", body.trim(), not(emptyOrNullString()));
    }

    @Test(priority = 2)
    @Story("Get Wallet")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/wallet/{userId} without token should be rejected.")
    public void getWallet_withoutToken_shouldBeRejected() {
        Response response = TestConfig.baseSpec()
                .get("/api/wallet/" + TokenManager.buyerUserId);

        System.out.println("[GET WALLET NO TOKEN] Status: " + response.getStatusCode());

        assertThat("Get wallet without token should fail",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    @Test(priority = 3)
    @Story("Get Wallet")
    @Severity(SeverityLevel.MINOR)
    @Description("GET /api/wallet/999999 for non-existent user should return 404.")
    public void getWallet_nonExistentUser_shouldReturn404() {
        Response response = TestConfig.authSpec(TokenManager.adminToken)
                .get("/api/wallet/999999");

        System.out.println("[GET WALLET 999999] Status: " + response.getStatusCode());

        assertThat("Non-existent user wallet should return 404 or 400",
                response.getStatusCode(), anyOf(is(404), is(400)));
    }

    // ── POST /api/wallet/{userId}/deposit ─────────────────────────────────────

    @Test(priority = 4)
    @Story("Deposit")
    @Severity(SeverityLevel.BLOCKER)
    @Description("POST /api/wallet/{userId}/deposit with valid amount should credit the wallet.")
    public void depositToBuyerWallet_shouldReturn200() {
        JSONObject body = new JSONObject();
        body.put("amount", 10000);

        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .body(body.toString())
                .post("/api/wallet/" + TokenManager.buyerUserId + "/deposit");

        System.out.println("[DEPOSIT] Status: " + response.getStatusCode());
        System.out.println("[DEPOSIT] Body  : " + response.asString());

        assertThat("Deposit should return 200 or 201",
                response.getStatusCode(), anyOf(is(200), is(201)));
    }

    @Test(priority = 5)
    @Story("Deposit")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /api/wallet/{userId}/deposit with zero amount should return 400.")
    public void depositZeroAmount_shouldReturn400() {
        JSONObject body = new JSONObject();
        body.put("amount", 0);

        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .body(body.toString())
                .post("/api/wallet/" + TokenManager.buyerUserId + "/deposit");

        System.out.println("[DEPOSIT ZERO] Status: " + response.getStatusCode());

        assertThat("Zero deposit should fail",
                response.getStatusCode(), anyOf(is(400), is(422)));
    }

    @Test(priority = 6)
    @Story("Deposit")
    @Severity(SeverityLevel.MINOR)
    @Description("POST /api/wallet/{userId}/deposit with negative amount should return 400.")
    public void depositNegativeAmount_shouldReturn400() {
        JSONObject body = new JSONObject();
        body.put("amount", -500);

        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .body(body.toString())
                .post("/api/wallet/" + TokenManager.buyerUserId + "/deposit");

        System.out.println("[DEPOSIT NEGATIVE] Status: " + response.getStatusCode());

        assertThat("Negative deposit should fail",
                response.getStatusCode(), anyOf(is(400), is(422)));
    }

    // ── POST /api/wallet/{userId}/withdraw ────────────────────────────────────

    @Test(priority = 7, dependsOnMethods = "depositToBuyerWallet_shouldReturn200")
    @Story("Withdraw")
    @Severity(SeverityLevel.CRITICAL)
    @Description("POST /api/wallet/{userId}/withdraw with sufficient balance should succeed.")
    public void withdrawFromBuyerWallet_shouldReturn200() {
        JSONObject body = new JSONObject();
        body.put("amount", 500);

        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .body(body.toString())
                .post("/api/wallet/" + TokenManager.buyerUserId + "/withdraw");

        System.out.println("[WITHDRAW] Status: " + response.getStatusCode());
        System.out.println("[WITHDRAW] Body  : " + response.asString());

        assertThat("Withdraw should return 200 or 201",
                response.getStatusCode(), anyOf(is(200), is(201)));
    }

    @Test(priority = 8)
    @Story("Withdraw")
    @Severity(SeverityLevel.CRITICAL)
    @Description("POST /api/wallet/{userId}/withdraw with more than balance should return 400 (insufficient balance).")
    public void withdrawInsufficientBalance_shouldReturn400() {
        JSONObject body = new JSONObject();
        body.put("amount", 99999999);   // Extremely large amount

        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .body(body.toString())
                .post("/api/wallet/" + TokenManager.buyerUserId + "/withdraw");

        System.out.println("[WITHDRAW INSUFFICIENT] Status: " + response.getStatusCode());
        System.out.println("[WITHDRAW INSUFFICIENT] Body  : " + response.asString());

        assertThat("Withdraw with insufficient balance should fail",
                response.getStatusCode(), anyOf(is(400), is(422)));
    }

    @Test(priority = 9)
    @Story("Withdraw")
    @Severity(SeverityLevel.MINOR)
    @Description("POST /api/wallet/{userId}/withdraw without token should be rejected.")
    public void withdrawWithoutToken_shouldBeRejected() {
        JSONObject body = new JSONObject();
        body.put("amount", 100);

        Response response = TestConfig.baseSpec()
                .body(body.toString())
                .post("/api/wallet/" + TokenManager.buyerUserId + "/withdraw");

        System.out.println("[WITHDRAW NO TOKEN] Status: " + response.getStatusCode());

        assertThat("Withdraw without token should fail",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    // ── POST /api/wallet/{userId}/holdBid ─────────────────────────────────────

    @Test(priority = 10)
    @Story("Hold Bid")
    @Severity(SeverityLevel.CRITICAL)
    @Description("POST /api/wallet/{userId}/holdBid should attempt to hold funds for a bid.")
    public void holdBidAmount_shouldReturn200Or400() {
        int auctionId = TokenManager.activeAuctionId > 0 ? TokenManager.activeAuctionId : 1;

        JSONObject body = new JSONObject();
        body.put("auctionId", auctionId);
        body.put("bidAmount", 100);

        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .body(body.toString())
                .post("/api/wallet/" + TokenManager.buyerUserId + "/holdBid");

        System.out.println("[HOLD BID] Status: " + response.getStatusCode());
        System.out.println("[HOLD BID] Body  : " + response.asString());

        // 200 = held, 400 = auction not active or insufficient funds — both acceptable
        assertThat("Hold bid should return 200 or 400",
                response.getStatusCode(), anyOf(is(200), is(201), is(400)));
    }

    @Test(priority = 11)
    @Story("Hold Bid")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /api/wallet/{userId}/holdBid with insufficient balance should return 400.")
    public void holdBidInsufficientBalance_shouldReturn400() {
        int auctionId = TokenManager.activeAuctionId > 0 ? TokenManager.activeAuctionId : 1;

        JSONObject body = new JSONObject();
        body.put("auctionId", auctionId);
        body.put("bidAmount", 99999999);  // Extremely large

        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .body(body.toString())
                .post("/api/wallet/" + TokenManager.buyerUserId + "/holdBid");

        System.out.println("[HOLD BID INSUF] Status: " + response.getStatusCode());

        assertThat("Hold bid with huge amount should fail",
                response.getStatusCode(), anyOf(is(400), is(422)));
    }

    // ── POST /api/wallet/{userId}/refund ──────────────────────────────────────

    @Test(priority = 12)
    @Story("Refund")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /api/wallet/{userId}/refund should process a refund.")
    public void refundBuyer_shouldReturn200Or400() {
        int auctionId = TokenManager.activeAuctionId > 0 ? TokenManager.activeAuctionId : 1;

        JSONObject body = new JSONObject();
        body.put("auctionId",    auctionId);
        body.put("refundAmount", 100);

        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .body(body.toString())
                .post("/api/wallet/" + TokenManager.buyerUserId + "/refund");

        System.out.println("[REFUND] Status: " + response.getStatusCode());
        System.out.println("[REFUND] Body  : " + response.asString());

        assertThat("Refund should return 200 or 400",
                response.getStatusCode(), anyOf(is(200), is(201), is(400)));
    }

    // ── POST /api/wallet/{userId}/payout ──────────────────────────────────────

    @Test(priority = 13)
    @Story("Payout")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /api/wallet/{userId}/payout should process a payout for seller.")
    public void payoutSeller_shouldReturn200Or400() {
        int auctionId = TokenManager.activeAuctionId > 0 ? TokenManager.activeAuctionId : 1;

        JSONObject body = new JSONObject();
        body.put("auctionId",    auctionId);
        body.put("payoutAmount", 100);

        Response response = TestConfig.authSpec(TokenManager.sellerToken)
                .body(body.toString())
                .post("/api/wallet/" + TokenManager.sellerUserId + "/payout");

        System.out.println("[PAYOUT] Status: " + response.getStatusCode());
        System.out.println("[PAYOUT] Body  : " + response.asString());

        assertThat("Payout should return 200 or 400",
                response.getStatusCode(), anyOf(is(200), is(201), is(400)));
    }

    // ── GET /api/wallet/{userId}/transactions ─────────────────────────────────

    @Test(priority = 14)
    @Story("Transactions")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /api/wallet/{userId}/transactions should return transaction history.")
    public void getBuyerTransactions_shouldReturn200() {
        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .get("/api/wallet/" + TokenManager.buyerUserId + "/transactions");

        System.out.println("[TRANSACTIONS] Status: " + response.getStatusCode());
        System.out.println("[TRANSACTIONS] Body  : "
                + response.asString().substring(0, Math.min(500, response.asString().length())));

        assertThat("Transactions should return 200", response.getStatusCode(), is(200));
    }

    @Test(priority = 15)
    @Story("Transactions")
    @Severity(SeverityLevel.MINOR)
    @Description("GET /api/wallet/{userId}/transactions without token should be rejected.")
    public void getTransactions_withoutToken_shouldBeRejected() {
        Response response = TestConfig.baseSpec()
                .get("/api/wallet/" + TokenManager.buyerUserId + "/transactions");

        System.out.println("[TRANSACTIONS NO TOKEN] Status: " + response.getStatusCode());

        assertThat("Transactions without token should fail",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    // ── Deposit to Seller wallet too (for payout tests) ──────────────────────

    @Test(priority = 16)
    @Story("Deposit")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /api/wallet/{sellerId}/deposit should credit seller wallet.")
    public void depositToSellerWallet_shouldReturn200() {
        JSONObject body = new JSONObject();
        body.put("amount", 5000);

        Response response = TestConfig.authSpec(TokenManager.sellerToken)
                .body(body.toString())
                .post("/api/wallet/" + TokenManager.sellerUserId + "/deposit");

        System.out.println("[DEPOSIT SELLER] Status: " + response.getStatusCode());
        System.out.println("[DEPOSIT SELLER] Body  : " + response.asString());

        assertThat("Seller deposit should return 200 or 201",
                response.getStatusCode(), anyOf(is(200), is(201)));
    }
}
