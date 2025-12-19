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
import hudson.Util;
import hudson.model.User;
import java.util.Date;

/**
 * Data transfer object containing information about an API token including its owner,
 * token details, and usage statistics. Used by the API token management page.
 *
 * @since 2.XXX
 */
public class AllTokenInfo {
    
    private final String userId;
    private final String userFullName;
    private final String tokenUuid;
    private final String tokenName;
    private final Date creationDate;
    private final long numDaysCreation;
    private final boolean isLegacy;
    private final int useCounter;
    private final Date lastUseDate;
    private final long numDaysUse;

    /**
     * Constructs an AllTokenInfo object containing comprehensive token information.
     *
     * @param user the user who owns this token
     * @param tokenUuid the unique identifier of the token
     * @param tokenName the name of the token
     * @param creationDate when the token was created
     * @param numDaysCreation number of days since creation
     * @param isLegacy whether this is a legacy token
     * @param useCounter number of times the token has been used
     * @param lastUseDate when the token was last used
     * @param numDaysUse number of days since last use
     */
    public AllTokenInfo(@NonNull User user, 
                        @NonNull String tokenUuid,
                        @NonNull String tokenName,
                        Date creationDate,
                        long numDaysCreation,
                        boolean isLegacy,
                        int useCounter,
                        Date lastUseDate,
                        long numDaysUse) {
        this.userId = user.getId();
        this.userFullName = user.getFullName();
        this.tokenUuid = tokenUuid;
        this.tokenName = tokenName;
        this.creationDate = creationDate;
        this.numDaysCreation = numDaysCreation;
        this.isLegacy = isLegacy;
        this.useCounter = useCounter;
        this.lastUseDate = lastUseDate;
        this.numDaysUse = numDaysUse;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public String getTokenUuid() {
        return tokenUuid;
    }

    public String getTokenName() {
        return tokenName;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public long getNumDaysCreation() {
        return numDaysCreation;
    }

    public boolean isLegacy() {
        return isLegacy;
    }

    public int getUseCounter() {
        return useCounter;
    }

    public Date getLastUseDate() {
        return lastUseDate;
    }

    public long getNumDaysUse() {
        return numDaysUse;
    }

    /**
     * Returns a human-readable string for how long ago the token was created.
     */
    public String getCreatedDaysAgo() {
        if (creationDate == null) {
            return "Unknown";
        }
        return formatDaysAgo(numDaysCreation, "created");
    }

    /**
     * Returns a human-readable string for how long ago the token was last used.
     */
    public String getLastUsedDaysAgo() {
        if (lastUseDate == null) {
            return "Never";
        }
        return formatDaysAgo(numDaysUse, "last used");
    }

    private String formatDaysAgo(long days, String prefix) {
        if (days == 0) {
            return prefix.substring(0, 1).toUpperCase() + prefix.substring(1) + " today";
        } else if (days == 1) {
            return prefix.substring(0, 1).toUpperCase() + prefix.substring(1) + " yesterday";
        } else if (days < 7) {
            return prefix.substring(0, 1).toUpperCase() + prefix.substring(1) + " " + days + " days ago";
        } else if (days < 14) {
            return prefix.substring(0, 1).toUpperCase() + prefix.substring(1) + " over a week ago";
        } else if (days < 30) {
            long weeks = days / 7;
            return prefix.substring(0, 1).toUpperCase() + prefix.substring(1) + " " + weeks + " weeks ago";
        } else if (days < 60) {
            return prefix.substring(0, 1).toUpperCase() + prefix.substring(1) + " over a month ago";
        } else if (days < 365) {
            long months = days / 30;
            return prefix.substring(0, 1).toUpperCase() + prefix.substring(1) + " " + months + " months ago";
        } else if (days < 730) {
            return prefix.substring(0, 1).toUpperCase() + prefix.substring(1) + " over a year ago";
        } else {
            long years = days / 365;
            return prefix.substring(0, 1).toUpperCase() + prefix.substring(1) + " " + years + " years ago";
        }
    }
}
