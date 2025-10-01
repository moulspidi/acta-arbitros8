
package cat.vbreferee.core.security;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class SecurePrefs {
    private final SharedPreferences prefs;
    private SecurePrefs(SharedPreferences prefs) { this.prefs = prefs; }
    public static SecurePrefs create(Context ctx, String fileName) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
        SharedPreferences sp = EncryptedSharedPreferences.create(
                ctx, fileName, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        return new SecurePrefs(sp);
    }
    public void putString(String k, String v) { prefs.edit().putString(k, v).apply(); }
    public String getString(String k, String d) { return prefs.getString(k, d); }
    public void putLong(String k, long v) { prefs.edit().putLong(k, v).apply(); }
    public long getLong(String k, long d) { return prefs.getLong(k, d); }
    public void putBoolean(String k, boolean v) { prefs.edit().putBoolean(k, v).apply(); }
    public boolean getBoolean(String k, boolean d) { return prefs.getBoolean(k, d); }
    public void remove(String k) { prefs.edit().remove(k).apply(); }
    public void clear() { prefs.edit().clear().apply(); }
}
