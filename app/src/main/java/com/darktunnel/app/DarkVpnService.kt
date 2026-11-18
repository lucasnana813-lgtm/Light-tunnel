package com.darktunnel.app

import android.content.Intent
import android.net.VpnService

/**
 * SQUELETTE du service VPN.
 * Ici il faut brancher le vrai cœur réseau (sing-box / Xray) configuré
 * avec ton serveur Trojan. Tant que ce n'est pas fait, aucun tunnel n'est créé
 * (donc ton internet normal continue de fonctionner).
 *
 * START -> Builder().addAddress("10.0.0.2",32).addRoute("0.0.0.0",0).addDnsServer("1.1.1.1").establish()
 *          puis donner le fd du TUN au cœur (config Trojan : serveur, port, mot de passe, SNI).
 * STOP  -> arrêter le cœur, fermer le TUN, stopSelf().
 */
class DarkVpnService : VpnService() {
    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        when (i?.action) {
            "START" -> { /* TODO: démarrer le cœur Trojan */ }
            "STOP" -> { /* TODO: arrêter le cœur */ stopSelf() }
        }
        return START_NOT_STICKY
    }
}
