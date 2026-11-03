// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package androidx.browser.auth;

import android.net.Uri;

public final class AuthResultTestFactory {
    private AuthResultTestFactory() {}

    public static AuthTabIntent.AuthResult create(int resultCode, Uri resultUri) {
        return new AuthTabIntent.AuthResult(resultCode, resultUri);
    }
}
