package com.l2jfrozen.gameserver.model.zone.type;

import com.l2jfrozen.gameserver.datatables.csv.MapRegionTable;
import com.l2jfrozen.gameserver.model.L2Character;
import com.l2jfrozen.gameserver.model.actor.instance.L2ItemInstance;
import com.l2jfrozen.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jfrozen.gameserver.model.actor.instance.L2PcInstance;
import com.l2jfrozen.gameserver.model.zone.L2ZoneType;
import com.l2jfrozen.util.L2FastList;
import javolution.util.FastMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by Server1 on 7/20/2017.
 */

public class L2PartyPvPZone extends L2ZoneType
{
    final static int minimumPlayers = 4;

    private String _zoneName;
    private int _timeInvade;
    private boolean _enabled = true; // default value, unless overridden by xml...
    private boolean _IsFlyingEnable = true; // default value, unless overridden by xml...
    private HashMap<Integer, Integer> playersPoints = new HashMap<Integer, Integer>();

    // track the times that players got disconnected. Players are allowed
    // to LOGGER back into the zone as long as their LOGGER-out was within _timeInvade
    // time...
    // <player objectId, expiration time in milliseconds>
    private final FastMap<Integer, Long> _playerAllowedReEntryTimes;

    // track the players admitted to the zone who should be allowed back in
    // after reboot/server downtime (outside of their control), within 30
    // of server restart
    private List<Integer> _playersAllowed;


    public L2PartyPvPZone(final int id)
    {
        super(id);
        _playerAllowedReEntryTimes = new FastMap<>();
        _playersAllowed = new L2FastList<>();
    }

    @Override
    public void setParameter(final String name, final String value)
    {
        if (name.equals("name"))
        {
            _zoneName = value;
        }
        else if (name.equals("InvadeTime"))
        {
            _timeInvade = Integer.parseInt(value);
        }
        else if (name.equals("EnabledByDefault"))
        {
            _enabled = Boolean.parseBoolean(value);
        }
        else if (name.equals("flying"))
        {
            _IsFlyingEnable = Boolean.parseBoolean(value);
        }
        else
        {
            super.setParameter(name, value);
        }
    }

    private boolean canEnter(L2PcInstance player)
    {
        if (player == null)
        {
            return false;
        }

        if (!player.isInParty())
        {
            player.sendMessage("You have to be in party of minimum " + minimumPlayers + " players in order to enter in this zone !");
            player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
            return false;
        }

        if (player.isInParty() && player.getParty().getMemberCount() < minimumPlayers)
        {
            for (L2PcInstance p : player.getParty().getPartyMembers())
            {
                if (p == null)
                    continue;
                p.sendMessage("You have to be in a party of minimum " + minimumPlayers + " in order ot enter in this zone !");
                p.teleToLocation(MapRegionTable.TeleportWhereType.Town);
            }
            return false;
        }

        return true;
    }

    @Override
    /**
     * Boss zones have special behaviors for player characters. Players are
     * automatically teleported out when the attempt to enter these zones,
     * except if the time at which they enter the zone is prior to the entry
     * expiration time set for that player. Entry expiration times are set by
     * any one of the following: 1) A player logs out while in a zone
     * (Expiration gets set to logoutTime + _timeInvade) 2) An external source
     * (such as a quest or AI of NPC) set up the player for entry.
     *
     * There exists one more case in which the player will be allowed to enter.
     * That is if the server recently rebooted (boot-up time more recent than
     * currentTime - _timeInvade) AND the player was in the zone prior to reboot.
     */



    protected void onEnter(final L2Character character)
    {


        if (character instanceof L2PcInstance)
        {
            L2PcInstance player = (L2PcInstance) character;

            if (!canEnter(player))
            {
                return;
            }

            if (!playersPoints.containsKey(player.getCharId()))
            {
                playersPoints.put(player.getCharId(), 0);
            }

            player.updatePvPFlag(1);
            // teleport out all players who attempt "illegal" (re-)entry
            player = null;
        }

    }

    /**
     * Some GrandBosses send all players in zone to a specific part of the zone, rather than just removing them all. If this is the case, this command should be used. If this is no the case, then use oustAllPlayers().
     * @param x
     * @param y
     * @param z
     */

    public void movePlayersTo(final int x, final int y, final int z)
    {
        if (_characterList.isEmpty())
            return;

        for (final L2Character character : _characterList.values())
        {
            if (character instanceof L2PcInstance)
            {
                final L2PcInstance player = (L2PcInstance) character;
                if (player.isOnline() == 1)
                    player.teleToLocation(x, y, z);
            }
        }
    }

    @Override
    protected void onExit(final L2Character character)
    {

            if (character instanceof L2PcInstance)
            {
                // Thread.dumpStack();
                L2PcInstance player = (L2PcInstance) character;

                if (playersPoints.containsKey(player.getCharId()))
                {
                    playersPoints.remove(player.getCharId());
                }

                if (player.isGM())
                {
                    player.sendMessage("You left " + _zoneName);
                    return;
                }

                player.stopPvPFlag();
                player = null;
            }

    }

    public void setZoneEnabled(final boolean flag)
    {
        if (_enabled != flag)
        {
            oustAllPlayers();
        }

        _enabled = flag;
    }

    public String getZoneName()
    {
        return _zoneName;
    }

    public int getTimeInvade()
    {
        return _timeInvade;
    }

    public void setAllowedPlayers(final List<Integer> list)
    {
        if (list != null)
        {
            _playersAllowed = list;
        }
    }

    public List<Integer> getAllowedPlayers()
    {
        return _playersAllowed;
    }

    public boolean isPlayerAllowed(final L2PcInstance player)
    {
        if (player.isGM())
            return true;
        else if (_playersAllowed.contains(player.getObjectId()))
            return true;
        else
        {
            player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
            return false;
        }
    }



    /**
     * Occasionally, all players need to be sent out of the zone (for example, if the players are just running around without fighting for too long, or if all players die, etc). This call sends all online players to town and marks offline players to be teleported (by clearing their relog expiration
     * times) when they LOGGER back in (no real need for off-line teleport).
     */
    public void oustAllPlayers()
    {
        if (_characterList == null)
            return;

        if (_characterList.isEmpty())
            return;

        for (final L2Character character : _characterList.values())
        {
            if (character == null)
            {
                continue;
            }

            if (character instanceof L2PcInstance)
            {
                L2PcInstance player = (L2PcInstance) character;

                if (player.isOnline() == 1)
                {
                    player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
                }

                player = null;
            }
        }
        _playerAllowedReEntryTimes.clear();
        _playersAllowed.clear();
    }

    /**
     * This function is to be used by external sources, such as quests and AI in order to allow a player for entry into the zone for some time. Naturally if the player does not enter within the allowed time, he/she will be teleported out again...
     * @param player reference to the player we wish to allow
     * @param durationInSec amount of time in seconds during which entry is valid.
     */
    public void allowPlayerEntry(final L2PcInstance player, final int durationInSec)
    {
        if (!player.isGM())
        {
            if (!_playersAllowed.contains(player.getObjectId()))
            {
                _playersAllowed.add(player.getObjectId());
            }
            _playerAllowedReEntryTimes.put(player.getObjectId(), System.currentTimeMillis() + durationInSec * 1000);
        }
    }

    @Override
    protected void onDieInside(final L2Character character)
    {
        if (character instanceof L2PcInstance) {
            if (!canEnter((L2PcInstance) character))
            {
                return;
            }
        }
    }

    @Override
    protected void onReviveInside(final L2Character character)
    {
        if (!canEnter((L2PcInstance) character))
            return;
        character.updatePvPFlag(1);
    }

    public void updateKnownList(final L2NpcInstance npc)
    {
        if (_characterList == null || _characterList.isEmpty())
            return;

        final Map<Integer, L2PcInstance> npcKnownPlayers = npc.getKnownList().getKnownPlayers();
        for (final L2Character character : _characterList.values())
        {
            if (character == null)
                continue;
            if (character instanceof L2PcInstance)
            {
                final L2PcInstance player = (L2PcInstance) character;
                if (player.isOnline() == 1 || player.isInOfflineMode()) // online players must be all taken into account, also offliner
                    npcKnownPlayers.put(player.getObjectId(), player);
            }
        }
        return;
    }

}
