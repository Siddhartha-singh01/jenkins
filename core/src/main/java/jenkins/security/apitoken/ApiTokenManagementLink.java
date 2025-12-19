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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.model.User;
import hudson.security.Permission;
import hudson.util.HttpResponses;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.security.ApiTokenProperty;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Management link that provides a centralized view of all API tokens across all users.
 * Allows administrators to view token usage statistics and revoke tokens for security
 * and compliance purposes.
 *
 * @since 2.XXX
 */
@Extension(ordinal = Integer.MAX_VALUE - 800)
@Symbol("apiTokens")
public class ApiTokenManagementLink extends ManagementLink {
    
    private static final Logger LOGGER = Logger.getLogger(ApiTokenManagementLink.class.getName());

    @Override
    public String getIconFileName() {
        return "symbol-key";
    }

    @Override
    public String getDisplayName() {
        return Messages.ApiTokenManagementLink_DisplayName();
    }

    @Override
    public String getDescription() {
        return Messages.ApiTokenManagementLink_Description();
    }

    @Override
    public String getUrlName() {
        return "api-tokens";
    }

    @NonNull
    @Override
    public Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
    }

    @NonNull
    @Override
    public Category getCategory() {
        return Category.SECURITY;
    }

    /**
     * Retrieves all API tokens from all users in the system.
     * 
     * @return a list of AllTokenInfo objects containing token details and statistics
     */
    public List<AllTokenInfo> getAllTokens() {
        List<AllTokenInfo> allTokens = new ArrayList<>();
        
        Collection<User> users = User.getAll();
        for (User user : users) {
            ApiTokenProperty tokenProperty = user.getProperty(ApiTokenProperty.class);
            if (tokenProperty == null) {
                continue;
            }
            
            ApiTokenStore tokenStore = tokenProperty.getTokenStore();
            ApiTokenStats tokenStats = tokenProperty.getTokenStats();
            
            Collection<ApiTokenStore.HashedToken> tokens = tokenStore.getTokenListSortedByName();
            for (ApiTokenStore.HashedToken token : tokens) {
                ApiTokenStats.SingleTokenStats stats = tokenStats.findTokenStatsById(token.getUuid());
                
                AllTokenInfo tokenInfo = new AllTokenInfo(
                    user,
                    token.getUuid(),
                    token.getName(),
                    token.getCreationDate(),
                    token.getNumDaysCreation(),
                    token.isLegacy(),
                    stats.getUseCounter(),
                    stats.getLastUseDate(),
                    stats.getNumDaysUse()
                );
                
                allTokens.add(tokenInfo);
            }
        }
        
        // Sort by user ID, then by token name
        allTokens.sort(Comparator.comparing(AllTokenInfo::getUserId)
                                 .thenComparing(AllTokenInfo::getTokenName));
        
        return allTokens;
    }

    /**
     * Deletes an API token for a specific user.
     * 
     * @param userId the ID of the user who owns the token
     * @param tokenUuid the UUID of the token to delete
     * @return HTTP response indicating success or failure
     */
    @RequirePOST
    public HttpResponse doDelete(@QueryParameter String userId, 
                                  @QueryParameter String tokenUuid) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        
        if (userId == null || userId.isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete token with null or empty userId");
            return HttpResponses.errorWithoutStack(400, "User ID is required");
        }
        
        if (tokenUuid == null || tokenUuid.isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete token with null or empty tokenUuid");
            return HttpResponses.errorWithoutStack(400, "Token UUID is required");
        }
        
        User user = User.getById(userId, false);
        if (user == null) {
            LOGGER.log(Level.WARNING, "User not found: {0}", userId);
            return HttpResponses.errorWithoutStack(404, "User not found: " + userId);
        }
        
        ApiTokenProperty tokenProperty = user.getProperty(ApiTokenProperty.class);
        if (tokenProperty == null) {
            LOGGER.log(Level.WARNING, "User {0} has no API token property", userId);
            return HttpResponses.errorWithoutStack(404, "User has no API tokens");
        }
        
        ApiTokenStore tokenStore = tokenProperty.getTokenStore();
        ApiTokenStats tokenStats = tokenProperty.getTokenStats();
        
        ApiTokenStore.HashedToken revokedToken = tokenStore.revokeToken(tokenUuid);
        if (revokedToken == null) {
            LOGGER.log(Level.WARNING, "Token {0} not found for user {1}", new Object[]{tokenUuid, userId});
            return HttpResponses.errorWithoutStack(404, "Token not found");
        }
        
        tokenStats.removeId(tokenUuid);
        
        try {
            user.save();
            LOGGER.log(Level.INFO, "Token {0} ({1}) revoked for user {2} by administrator", 
                      new Object[]{revokedToken.getName(), tokenUuid, userId});
            return HttpResponses.redirectTo(".");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save user after token deletion", e);
            return HttpResponses.error(500, "Failed to save changes: " + e.getMessage());
        }
    }
}
