/*
 * The MIT License
 *
 * Copyright (c) 2024
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.security.apitoken;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import hudson.model.User;
import hudson.model.ManagementLink;
import java.util.List;
import jenkins.model.Jenkins;
import jenkins.security.ApiTokenProperty;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.springframework.security.access.AccessDeniedException;

/**
 * Tests for {@link ApiTokenManagementLink}.
 */
public class ApiTokenManagementLinkTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private ApiTokenManagementLink link;

    @Before
    public void setUp() {
        link = new ApiTokenManagementLink();
    }

    @Test
    public void testLinkProperties() {
        assertThat(link.getIconFileName(), is("symbol-key"));
        assertThat(link.getUrlName(), is("api-tokens"));
        assertThat(link.getCategory(), is(ManagementLink.Category.SECURITY));
        assertThat(link.getRequiredPermission(), is(Jenkins.ADMINISTER));
        assertThat(link.getDisplayName(), is(notNullValue()));
        assertThat(link.getDescription(), is(notNullValue()));
    }

    @Test
    public void testGetAllTokensEmpty() {
        List<AllTokenInfo> tokens = link.getAllTokens();
        assertThat(tokens, is(notNullValue()));
        // May be empty if no users have tokens
    }

    @Test
    public void testGetAllTokensWithUsers() throws Exception {
        // Create a user and add some tokens
        User user1 = User.getById("user1", true);
        ApiTokenProperty tokenProperty1 = new ApiTokenProperty();
        user1.addProperty(tokenProperty1);
        
        // Generate a token
        ApiTokenStore.TokenUuidAndPlainValue token1 = tokenProperty1.getTokenStore().generateNewToken("test-token-1");
        assertThat(token1, is(notNullValue()));

        User user2 = User.getById("user2", true);
        ApiTokenProperty tokenProperty2 = new ApiTokenProperty();
        user2.addProperty(tokenProperty2);
        
        ApiTokenStore.TokenUuidAndPlainValue token2 = tokenProperty2.getTokenStore().generateNewToken("test-token-2");
        assertThat(token2, is(notNullValue()));

        // Get all tokens
        List<AllTokenInfo> allTokens = link.getAllTokens();
        
        assertThat(allTokens, is(not(empty())));
        assertThat(allTokens.size(), is(greaterThan(0)));
        
        // Verify token information is populated correctly
        boolean foundUser1Token = false;
        boolean foundUser2Token = false;
        
        for (AllTokenInfo tokenInfo : allTokens) {
            if (tokenInfo.getUserId().equals("user1") && tokenInfo.getTokenName().equals("test-token-1")) {
                foundUser1Token = true;
                assertThat(tokenInfo.getTokenUuid(), is(notNullValue()));
                assertThat(tokenInfo.getUserFullName(), is(notNullValue()));
            }
            if (tokenInfo.getUserId().equals("user2") && tokenInfo.getTokenName().equals("test-token-2")) {
                foundUser2Token = true;
                assertThat(tokenInfo.getTokenUuid(), is(notNullValue()));
                assertThat(tokenInfo.getUserFullName(), is(notNullValue()));
            }
        }
        
        assertTrue("Should find user1's token", foundUser1Token);
        assertTrue("Should find user2's token", foundUser2Token);
    }

    @Test
    public void testDeleteToken() throws Exception {
        // Create a user with a token
        User user = User.getById("testuser", true);
        ApiTokenProperty tokenProperty = new ApiTokenProperty();
        user.addProperty(tokenProperty);
        
        ApiTokenStore.TokenUuidAndPlainValue token = tokenProperty.getTokenStore().generateNewToken("test-token");
        String tokenUuid = token.tokenUuid;
        
        // Verify token exists
        List<AllTokenInfo> tokensBefore = link.getAllTokens();
        long countBefore = tokensBefore.stream()
            .filter(t -> t.getTokenUuid().equals(tokenUuid))
            .count();
        assertThat(countBefore, is(1L));
        
        // Delete the token as admin
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.checkPermission(Jenkins.ADMINISTER); // Should not throw
        
        link.doDelete("testuser", tokenUuid);
        
        // Verify token is deleted
        List<AllTokenInfo> tokensAfter = link.getAllTokens();
        long countAfter = tokensAfter.stream()
            .filter(t -> t.getTokenUuid().equals(tokenUuid))
            .count();
        assertThat(countAfter, is(0L));
    }

    @Test
    public void testDeleteTokenInvalidUser() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        
        try {
            link.doDelete("nonexistentuser", "some-uuid");
            fail("Should have thrown an error for non-existent user");
        } catch (Exception e) {
            // Expected - the doDelete returns an error response
            assertThat(e.getMessage(), containsString("User not found"));
        }
    }

    @Test
    public void testAllTokenInfoFormatting() throws Exception {
        User user = User.getById("formattest", true);
        ApiTokenProperty tokenProperty = new ApiTokenProperty();
        user.addProperty(tokenProperty);
        
        ApiTokenStore.TokenUuidAndPlainValue token = tokenProperty.getTokenStore().generateNewToken("format-test-token");
        
        List<AllTokenInfo> tokens = link.getAllTokens();
        AllTokenInfo tokenInfo = tokens.stream()
            .filter(t -> t.getTokenName().equals("format-test-token"))
            .findFirst()
            .orElse(null);
        
        assertNotNull("Token should be found", tokenInfo);
        
        // Test date formatting methods
        String createdAgo = tokenInfo.getCreatedDaysAgo();
        assertThat(createdAgo, is(notNullValue()));
        assertThat(createdAgo, containsString("today")); // Just created
        
        String lastUsedAgo = tokenInfo.getLastUsedDaysAgo();
        assertThat(lastUsedAgo, is("Never")); // Not used yet
    }
}
