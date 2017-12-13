/*
 * Copyright 2014-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mycelium.spvmodule

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.Log

import java.net.InetAddress

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
    private var application: SpvModuleApplication? = null
    private var config: Configuration? = null
    private var pm: PackageManager? = null

    private val handler = Handler()
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var trustedPeerPreference: Preference? = null
    private var trustedPeerOnlyPreference: Preference? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.application = SpvModuleApplication.getApplication()
        this.config = application!!.configuration
        this.pm = context.packageManager
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_settings)

        backgroundThread = HandlerThread("backgroundThread", Process.THREAD_PRIORITY_BACKGROUND)
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)

        trustedPeerPreference = findPreference(Configuration.PREFS_KEY_TRUSTED_PEER)
        trustedPeerPreference!!.onPreferenceChangeListener = this

        trustedPeerOnlyPreference = findPreference(Configuration.PREFS_KEY_TRUSTED_PEER_ONLY)
        trustedPeerOnlyPreference!!.onPreferenceChangeListener = this

        val dataUsagePreference = findPreference(Configuration.PREFS_KEY_DATA_USAGE)
        dataUsagePreference.isEnabled = pm!!.resolveActivity(dataUsagePreference.intent, 0) != null

        updateTrustedPeer()
    }

    override fun onDestroy() {
        trustedPeerOnlyPreference!!.onPreferenceChangeListener = null
        trustedPeerPreference!!.onPreferenceChangeListener = null

        backgroundThread!!.looper.quit()

        super.onDestroy()
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        // delay action because preference isn't persisted until after this method returns
        handler.post {
            if (preference == trustedPeerPreference) {
                application!!.stopBlockchainService()
                updateTrustedPeer()
            } else if (preference == trustedPeerOnlyPreference) {
                application!!.stopBlockchainService()
            }
        }
        return true
    }

    private fun updateTrustedPeer() {
        val trustedPeer = config!!.trustedPeerHost

        if (trustedPeer == null) {
            trustedPeerPreference!!.setSummary(R.string.preferences_trusted_peer_summary)
            trustedPeerOnlyPreference!!.isEnabled = false
        } else {
            trustedPeerPreference!!.summary = trustedPeer + "\n[" + getString(R.string.preferences_trusted_peer_resolve_progress) + "]"
            trustedPeerOnlyPreference!!.isEnabled = true

            object : ResolveDnsTask(backgroundHandler!!) {
                override fun onSuccess(address: InetAddress) {
                    trustedPeerPreference!!.summary = trustedPeer
                    Log.i(LOG_TAG, "trusted peer '$trustedPeer' resolved to $address")
                }

                override fun onUnknownHost() {
                    trustedPeerPreference!!.summary = trustedPeer + "\n[" + getString(R.string.preferences_trusted_peer_resolve_unknown_host) + "]"
                }
            }.resolve(trustedPeer)
        }
    }
    companion object {
        val LOG_TAG = "SettingsFragment"
    }
}
