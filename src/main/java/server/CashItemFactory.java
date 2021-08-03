package server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

import database.DatabaseConnection;
import handling.cashshop.CashShopServer;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.CashItemInfo.CashModInfo;
import tools.wztosql.AddCashItemToDB;

public class CashItemFactory {

    private final static CashItemFactory instance = new CashItemFactory();
    private final static int[] bestItems = new int[]{50100010, 50100010, 50100010, 50100010, 50100010};
    private final static int[] bestItems2 = new int[]{50100010, 50100010, 50100010, 50100010, 50100010};
    private boolean initialized = false;
    private boolean initialized2 = false;
    private final Map<Integer, CashItemInfo> itemStats = new HashMap<>();
    private final Map<Integer, CashItemInfo> itemStats2 = new HashMap<>();
    private final Map<Integer, List<CashItemInfo>> itemPackage = new HashMap<>();
    private final Map<Integer, List<CashItemInfo>> itemPackage2 = new HashMap<>();
    private final Map<Integer, CashModInfo> itemMods = new HashMap<>();
    private final Map<Integer, CashModInfo> itemMods2 = new HashMap<>();
    private final MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath", "wz") + "/Etc.wz"));
    //是这个目录把，嗯
    private final MapleDataProvider itemStringInfo = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath", "wz") + "/String.wz"));
    private final Map<Integer, Integer> idLookup = new HashMap();
    private final Map<Integer, Integer> idLookup2 = new HashMap();
    private static final Map<Integer, List<CashItemInfo>> cashPackages = new HashMap();
    private static final Map<Integer, List<CashItemInfo>> cashPackages2 = new HashMap();

    public static final CashItemFactory getInstance() {
        return instance;
    }

    protected CashItemFactory() {
    }

    public void initialize() {
        //System.out.println("商城 :::");
        final List<Integer> itemids = new ArrayList<>();
        for (MapleData field : data.getData("Commodity.img").getChildren()) {
            final int SN = MapleDataTool.getIntConvert("SN", field, 0);
            final int itemId = MapleDataTool.getIntConvert("ItemId", field, 0);
            final CashItemInfo stats = new CashItemInfo(itemId,
                    MapleDataTool.getIntConvert("Count", field, 1),
                    MapleDataTool.getIntConvert("Price", field, 0), SN,
                    MapleDataTool.getIntConvert("Period", field, 0),
                    MapleDataTool.getIntConvert("Gender", field, 2),
                    MapleDataTool.getIntConvert("OnSale", field, 0) > 0);

            if (SN > 0) {
                itemStats.put(SN, stats);
                idLookup.put(itemId, SN);
            }
            if (itemId > 0) {
                itemids.add(itemId);
            }
        }
        for (int i : itemids) {
            getPackageItems(i);
        }
        for (int i : itemStats.keySet()) {
            getModInfo(i);
            getItem(i); //init the modinfo's citem
        }
        initialized = true;
    }

    public void initialize2() {
        //System.out.println("商城 :::");
        final List<Integer> itemids = new ArrayList<>();
        for (MapleData field : data.getData("Commodity.img").getChildren()) {
            final int SN = MapleDataTool.getIntConvert("SN", field, 0);
            final int itemId = MapleDataTool.getIntConvert("ItemId", field, 0);
            final CashItemInfo stats = new CashItemInfo(itemId,
                    MapleDataTool.getIntConvert("Count", field, 1),
                    MapleDataTool.getIntConvert("Price", field, 0), SN,
                    MapleDataTool.getIntConvert("Period", field, 0),
                    MapleDataTool.getIntConvert("Gender", field, 2),
                    MapleDataTool.getIntConvert("OnSale", field, 0) > 0);

            if (SN > 0) {
                itemStats2.put(SN, stats);
                idLookup2.put(itemId, SN);
            }

            if (itemId > 0) {
                itemids.add(itemId);
            }
        }
        for (int i : itemids) {
            getPackageItems2(i);
        }
        for (int i : itemStats2.keySet()) {
            getModInfo2(i);
            getItem2(i); //init the modinfo's citem
        }
        initialized2 = true;
    }

    public final CashItemInfo getItem(int sn) {
        final CashItemInfo stats = itemStats.get(sn);
        // final CashItemInfo stats = itemStats.get(Integer.valueOf(sn));
        final CashModInfo z = getModInfo(sn);
        if (z != null && z.showUp) {
            return z.toCItem(stats); //null doesnt matter
        }
        if (stats == null || !stats.onSale()) {
            return null;
        }
        //hmm
        return stats;
    }

    public final CashItemInfo getItem2(int sn) {
        final CashItemInfo stats = itemStats2.get(sn);
        // final CashItemInfo stats = itemStats.get(Integer.valueOf(sn));
        final CashModInfo z = getModInfo2(sn);
        if (z != null && z.showUp) {
            return z.toCItem(stats); //null doesnt matter
        }
        if (stats == null || !stats.onSale()) {
            return null;
        }
        //hmm
        return stats;
    }

    public static List<CashItemInfo> getPackageItems(int itemId) {
        if (cashPackages.containsKey(itemId)) {
            return cashPackages.get(itemId);
        }
        List<CashItemInfo> packageItems = new ArrayList<CashItemInfo>();
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath", "wz") + "/" + "Etc.wz"));
        MapleData a = dataProvider.getData("CashPackage.img");
        for (MapleData b : a.getChildren()) {
            if (itemId == Integer.parseInt(b.getName())) {
                for (MapleData c : b.getChildren()) {
                    for (MapleData d : c.getChildren()) {
                        int SN = MapleDataTool.getIntConvert("" + Integer.parseInt(d.getName()), c);
                        //packageItems.add(getItem(SN));
                        cashPackages.put(itemId, packageItems);
                    }
                }
                break;
            }
        }
        cashPackages.put(itemId, packageItems);
        return packageItems;
    }

    public static List<CashItemInfo> getPackageItems2(int itemId) {
        if (cashPackages2.containsKey(itemId)) {
            return cashPackages2.get(itemId);
        }
        List<CashItemInfo> packageItems = new ArrayList<>();
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath", "wz") + "/" + "Etc.wz"));
        MapleData a = dataProvider.getData("CashPackage.img");
        for (MapleData b : a.getChildren()) {
            if (itemId == Integer.parseInt(b.getName())) {
                for (MapleData c : b.getChildren()) {
                    for (MapleData d : c.getChildren()) {
                        int SN = MapleDataTool.getIntConvert("" + Integer.parseInt(d.getName()), c);
                        //packageItems.add(getItem(SN));
                        cashPackages2.put(itemId, packageItems);
                    }
                }
                break;
            }
        }
        cashPackages2.put(itemId, packageItems);
        return packageItems;
    }

    public final CashModInfo getModInfo(int sn) {
        CashModInfo ret = itemMods.get(sn);
        //  System.out.println(itemMods.toString());
        if (ret == null) {
            if (initialized) {
                return null;
            }
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM cashshop_modified_items WHERE serial = ?");
                ps.setInt(1, sn);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    ret = new CashModInfo(sn, rs.getInt("discount_price"), rs.getInt("mark"), rs.getInt("showup") > 0, rs.getInt("itemid"), rs.getInt("priority"), rs.getInt("package") > 0, rs.getInt("period"), rs.getInt("gender"), rs.getInt("count"), rs.getInt("meso"), rs.getInt("unk_1"), rs.getInt("unk_2"), rs.getInt("unk_3"), rs.getInt("extra_flags"));
                    itemMods.put(sn, ret);
                }
                rs.close();
                ps.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public final CashModInfo getModInfo2(int sn) {
        CashModInfo ret = itemMods2.get(sn);
        if (ret == null) {
            if (initialized2) {
                return null;
            }
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM cashshop_modified_items2 WHERE serial = ?");
                ps.setInt(1, sn);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    ret = new CashModInfo(sn, rs.getInt("discount_price"), rs.getInt("mark"), rs.getInt("showup") > 0, rs.getInt("itemid"), rs.getInt("priority"), rs.getInt("package") > 0, rs.getInt("period"), rs.getInt("gender"), rs.getInt("count"), rs.getInt("meso"), rs.getInt("unk_1"), rs.getInt("unk_2"), rs.getInt("unk_3"), rs.getInt("extra_flags"));
                    itemMods2.put(sn, ret);
                }
                rs.close();
                ps.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public final Collection<CashModInfo> getAllModInfo() {
        if (!initialized) {
            initialize();
        }
        return itemMods.values();
    }

    public final Collection<CashModInfo> getAllModInfo2() {
        if (!initialized2) {
            initialize2();
        }
        return itemMods2.values();
    }

    public final int[] getBestItems() {
        return bestItems;
    }
    public final int[] getBestItems2() {
        return bestItems2;
    }

    public int getSnFromId(int itemId) {
        return idLookup.get(itemId);
    }

    public int getSnFromId2(int itemId) {
        return idLookup2.get(itemId);
    }

    public final void clearCashShop() {
        itemStats.clear();
        itemPackage.clear();
        itemMods.clear();
        idLookup.clear();
        initialized = false;
        initialize();
        itemStats2.clear();
        itemPackage2.clear();
        itemMods2.clear();
        idLookup2.clear();
        initialized2 = false;
        initialize2();
    }

    public final int getItemSN(int itemid) {
        for (Map.Entry<Integer, CashItemInfo> ci : itemStats.entrySet()) {
            if (ci.getValue().getId() == itemid) {
                return ci.getValue().getSN();
            }
        }
        return 0;
    }
}
