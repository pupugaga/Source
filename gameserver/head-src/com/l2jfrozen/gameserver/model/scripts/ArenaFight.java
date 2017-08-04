package com.l2jfrozen.gameserver.model.scripts;

import com.l2jfrozen.Config;
import com.l2jfrozen.gameserver.datatables.sql.NpcTable;
import com.l2jfrozen.gameserver.datatables.sql.SpawnTable;
import com.l2jfrozen.gameserver.idfactory.IdFactory;
import com.l2jfrozen.gameserver.managers.GrandBossManager;
import com.l2jfrozen.gameserver.managers.RaidBossSpawnManager;
import com.l2jfrozen.gameserver.model.L2Object;
import com.l2jfrozen.gameserver.model.L2World;
import com.l2jfrozen.gameserver.model.actor.instance.L2ArenaManagerInstance;
import com.l2jfrozen.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jfrozen.gameserver.model.actor.instance.L2PcInstance;
import com.l2jfrozen.gameserver.model.entity.Announcements;
import com.l2jfrozen.gameserver.model.quest.QuestSpawn;
import com.l2jfrozen.gameserver.model.spawn.L2Spawn;
import com.l2jfrozen.gameserver.network.SystemMessageId;
import com.l2jfrozen.gameserver.network.serverpackets.CustomNpcInfo;
import com.l2jfrozen.gameserver.network.serverpackets.MagicSkillUser;
import com.l2jfrozen.gameserver.network.serverpackets.SystemMessage;
import com.l2jfrozen.gameserver.powerpak.Buffer.L2BufferInstance;
import com.l2jfrozen.gameserver.templates.L2NpcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by Server1 on 8/3/2017.
 */
public class ArenaFight {


    private Vector<L2PcInstance> fighters = new Vector<L2PcInstance>();
    private int[][] spawns;
    private int idArena;
    private int idFight;
    private L2ArenaManagerInstance manager;

    private Vector<L2Spawn> buffers = new Vector<L2Spawn>();

    private boolean betActivated = false;
    private HashMap<Integer, long[] > bets = new HashMap<Integer, long[]>();

    public boolean isStarted = false;

    public ArenaFight( L2PcInstance playerOne, L2PcInstance playerTwo, int idArena, int idFight) {
        fighters.add(playerOne);
        fighters.add(playerTwo);
        this.idArena = idArena;
        this.idFight = idFight;
    }

    public void teleportPlayers() {
        int count = 0;

        for (L2PcInstance p : fighters) {

            p.teleToLocation(spawns[count][0], spawns[count][1], spawns[count][2]);
            count++;
        }
    }

    public void startFight() {

        Announcements.getInstance().announceToAll("Arena: "+idArena);
        betActivated = false;
        unspawnBuffers();
        isStarted = true;
        Announcements.getInstance().announceToAll("Fight : " + fighters.firstElement() + " vs " + fighters.lastElement());
        for (L2PcInstance p : fighters) {

            p.sendMessage("Fight to the death!!");
            p.setIsImobilised(false);
        }

        new java.util.Timer().schedule(

                new java.util.TimerTask() {
                    @Override
                    public void run() {
                    if (isStarted)
                    {
                        if (!fighters.isEmpty()) {
                                    for (L2PcInstance p : fighters)
                                    {
                                        p.sendMessage("TIE !");
                                    }
                        }
                        endGame();
                    }
                        this.cancel();

                    }
                },
                120000
        );


    }

    public void endGame()
    {
        if (fighters.isEmpty())
        {
            return;
        }
        for (L2PcInstance p : fighters) {

            if (p.isDead())
            {
                p.doRevive();
                p.setCurrentHp(p.getMaxHp());
                p.setCurrentMp(p.getMaxMp());
                p.setCurrentCp(p.getMaxCp());
            }
            p.teleToLocation(spawns[0][0], spawns[0][1], spawns[0][2]);
        }

        L2PcInstance winner = null;

        if (fighters.firstElement() != null && !fighters.firstElement().isDead()) {
            winner = fighters.firstElement();
            giveBetRewards(winner);
        }
        fighters.clear();
        L2ArenaManagerInstance.fights.remove(this);
    }

    private void loadSpawns() {
        spawns = new int[2][3];
        spawns[0][0] = -75128;
        spawns[0][1] = -239144;
        spawns[0][2] = -8208 ;

        spawns[1][0] = -75128;
        spawns[1][1] = -239144;
        spawns[1][2] = -3495;
    }

    public void beginArena() {


        manager.refreshArenaAvailability(idArena,false);
        betActivated = true;
        loadSpawns();
        spawnBuffers();
        teleportPlayers();
        for (L2PcInstance p : fighters) {

            p.sendMessage("You have one minute to prepare");
            p.setIsImobilised(true);
        }

        new java.util.Timer().schedule(

                new java.util.TimerTask() {
                    @Override
                    public void run() {

                        startFight();
                        this.cancel();

                    }
                },
                20000
        );


    }


    public void giveBetRewards(L2PcInstance winner){

        for (Map.Entry<Integer, long[]> p : bets.entrySet()){

            L2PcInstance player = L2World.getInstance().getPlayer(p.getKey());
            long[] betInfos = betInfos = p.getValue();

            if (winner.getObjectId() == betInfos[1]){
                player.sendMessage("You won ur bet of " + betInfos[0]);
            }
        }

    }

    private void spawnBuffers() {



        L2NpcTemplate template1;
        template1 = NpcTable.getInstance().getTemplate(50019);

        int count = 0;
        try {
            for (int i=0; i<2;i++) {

                L2Spawn  b = new L2Spawn(template1);
                if (Config.SAVE_GMSPAWN_ON_CUSTOM)
                    b.setCustom(true);
                b.setLocx(spawns[count][0] + 40);
                b.setLocy(spawns[count][1]);
                b.setLocz(spawns[count][2]);
                b.setAmount(1);
                b.setHeading(0);
                b.setRespawnDelay(1);

                SpawnTable.getInstance().addNewSpawn(b, false);


                b.init();
                buffers.add(b);
                count++;
                b = null;
            }
        } catch (final Exception e) {
            if (Config.ENABLE_ALL_EXCEPTIONS)
                e.printStackTrace();

        }

        template1 = null;

    }


    private void unspawnBuffers() {
        for (L2Spawn b : buffers) {

            if (b == null || b.getLastSpawn() == null)
                return;

            b.getLastSpawn().deleteMe();
            b.stopRespawn();
            SpawnTable.getInstance().deleteSpawn(b, false);

        }
    }

    public void addBet(L2PcInstance player, long Amount, long winner)
    {

        if (!betActivated)
        {
            player.sendMessage("Bets are off.");
            return;
        }

        if (bets.containsKey(player.getObjectId()))
        {
            player.sendMessage("You have already bet");
            return;
        }
        long[] infos = new long[3];

        infos[0] = Amount;
        infos[1] = winner;

        bets.put(player.getObjectId(), infos);
    }



    public Vector<L2PcInstance> getFighters() {
        return fighters;
    }

    public int getIdFight() {
        return idFight;
    }



}
