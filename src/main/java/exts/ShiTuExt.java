package exts;

import client.MapleCharacter;
import exts.model.ShiTu;
import exts.model.ShiTuCharacter;
import exts.model.ShiTuRenwu;
import exts.model.ShiTuShop;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface ShiTuExt {

    //创建师门
    static boolean create(MapleCharacter mc, String name) {
        if (exists(name)) {
            return false;
        }

        int shituId = insert(mc, name);
        if (shituId <= 0L) {
            return false;
        }

        return insertCharacter(shituId, mc, 3);
    }

    //加入师门申请
    static boolean joinApply(MapleCharacter mc, int shituId) {
        if (!exists(shituId)) {
            return false;
        }

        if (existsCharacter(shituId, mc.getId())) {
            return true;
        }

        return insertCharacter(shituId, mc, 1);
    }

    //加入师门通过
    static boolean joinPassByCharacterName(int shituId, String characterName) {
        if (!exists(shituId)) {
            return false;
        }

        return updateCharacterByName(shituId, characterName, 2, 1);
    }

    //解散师门
    static boolean dismiss(int shituId) {
        if (!exists(shituId)) {
            return false;
        }

        deleteById(shituId);
        deleteCharacterByShiTuId(shituId);
        deleteShopByShiTuId(shituId);
        return true;
    }

    //退出师门
    static boolean exit(int shituId, String characterName) {
        if (!exists(shituId)) {
            return false;
        }

        return deleteCharacterByShiTuIdAndCharacterName(shituId, characterName);
    }

    //续费
    static boolean renew(int shituId) {
        if (!exists(shituId)) {
            return false;
        }

        return updateExpireTimeById(shituId);
    }

    //师门是否存在
    static boolean exists(String name) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("SELECT COUNT(*) as c FROM shitu AS s WHERE s.name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int ret = rs.getInt("c");
                rs.close();
                ps.close();
                return ret > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //师门是否存在
    static boolean exists(int id) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("SELECT COUNT(*) as c FROM shitu AS s WHERE s.id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int ret = rs.getInt("c");
                rs.close();
                ps.close();
                return ret > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //师门成员是否存在
    static boolean existsCharacter(int shituId, int characterId) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("SELECT COUNT(*) as c FROM shitu_character AS s WHERE s.shitu_id = ? and s.character_id = ?");
            ps.setInt(1, shituId);
            ps.setInt(2, characterId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int ret = rs.getInt("c");
                rs.close();
                ps.close();
                return ret > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //师门成员是否存在
    static boolean existsCharacterByName(int shituId, String characterName) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("SELECT COUNT(*) as c FROM shitu_character AS s WHERE s.shitu_id = ? and s.character_name = ?");
            ps.setInt(1, shituId);
            ps.setString(2, characterName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int ret = rs.getInt("c");
                rs.close();
                ps.close();
                return ret > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //插入师门
    static int insert(MapleCharacter mc, String name) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("INSERT INTO shitu(`name`,master_id,master_name,create_time,expire_time) values (?,?,?,NOW(),ADDDATE(NOW(), 30))", PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setInt(2, mc.getId());
            ps.setString(3, mc.getName());
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                rs.close();
                ps.close();
                return id;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    //插入师门成员
    static boolean insertCharacter(int shituId, MapleCharacter mc, int state) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("INSERT INTO shitu_character(shitu_id,character_id,character_name,state,join_time) values (?,?,?,?,NOW())");
            ps.setInt(1, shituId);
            ps.setInt(2, mc.getId());
            ps.setString(3, mc.getName());
            ps.setInt(4, state);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //修改师门成员
    //类型（1申请中2师门成员3掌门人4副掌门人）
    static boolean updateCharacter(int shituId, String characterName, int state, int oldstate) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu_character AS sc SET sc.state = ? WHERE sc.shitu_id = ? AND sc.character_name = ? AND sc.state = ?");
            ps.setInt(1, state);
            ps.setInt(2, shituId);
            ps.setString(3, characterName);
            ps.setInt(4, oldstate);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean updateShituSecond(int shituId, int secondId, String secondName) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu AS s SET s.second_id = ?,s.second_name = ? WHERE s.id = ?");
            ps.setInt(1, secondId);
            ps.setString(2, secondName);
            ps.setInt(3, shituId);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //修改师门成员
    static boolean updateCharacterByName(int shituId, String characterName, int state, int oldstate) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu_character AS sc SET sc.state = ? WHERE sc.shitu_id = ? AND sc.character_name = ? AND sc.state = ?");
            ps.setInt(1, state);
            ps.setInt(2, shituId);
            ps.setString(3, characterName);
            ps.setInt(4, oldstate);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //查询所有师门
    static List<ShiTu> selectAll() {
        PreparedStatement ps;
        List<ShiTu> cs = new ArrayList<>();
        try {
            ps = ConnExt.getConn().prepareStatement("SELECT s.*,DATE_FORMAT(s.create_time, '%Y-%m-%d %T') as createTime,DATE_FORMAT(s.expire_time, '%Y-%m-%d %T') as expiretime FROM shitu AS s order by s.contribution desc");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cs.add(new ShiTu(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cs;
    }

    //查询师门
    static ShiTu selectById(int shituId) {
        if (!exists(shituId)) {
            return null;
        }

        PreparedStatement ps;
        List<ShiTu> cs = new ArrayList<>();
        try {
            ps = ConnExt.getConn().prepareStatement("SELECT s.*,DATE_FORMAT(s.create_time, '%Y-%m-%d %T') as createtime,DATE_FORMAT(s.expire_time, '%Y-%m-%d %T') as expiretime FROM shitu AS s WHERE s.id = ?");
            ps.setInt(1, shituId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cs.add(new ShiTu(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cs.size() > 0 ? cs.get(0) : null;
    }

    //修改师门
    static boolean updateById(int shituId, int reward, int place, int stock) {
        if (!exists(shituId)) {
            return false;
        }

        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu AS s SET s.reward = ?, s.place = ?, s.stock = ? WHERE s.id = ?");
            ps.setInt(1, reward);
            ps.setInt(2, place);
            ps.setInt(3, stock);
            ps.setInt(4, shituId);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean updateContribution(int shituId, int cont) {
        if (!exists(shituId)) {
            return false;
        }

        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu AS s SET s.contribution = s.contribution + ? WHERE s.id = ?");
            ps.setInt(1, cont);
            ps.setInt(2, shituId);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean updateCharContribution(int shituId, int charid, int cont) {
        if (!exists(shituId) || !existsCharacter(shituId, charid)) {
            return false;
        }

        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu_character AS s SET s.contribution = s.contribution + ? WHERE s.shitu_id = ? and s.character_id = ?");
            ps.setInt(1, cont);
            ps.setInt(2, shituId);
            ps.setInt(3, charid);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean updateCharMeso(int shituId, int charid, int meso) {
        if (!exists(shituId) || !existsCharacter(shituId, charid)) {
            return false;
        }

        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu_character AS s SET s.meso = s.meso + ? WHERE s.shitu_id = ? and s.character_id = ?");
            ps.setInt(1, meso);
            ps.setInt(2, shituId);
            ps.setInt(3, charid);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //修改师门公告
    static boolean updateNoteById(int shituId, String note) {
        if (!exists(shituId)) {
            return false;
        }

        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu AS s SET s.note = ? WHERE s.id = ?");
            ps.setString(1, note);
            ps.setInt(2, shituId);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //修改师门名称
    static boolean updateNameById(int shituId, String name) {
        if (exists(name)) {
            return false;
        }

        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu AS s SET s.name = ? WHERE s.id = ?");
            ps.setString(1, name);
            ps.setInt(2, shituId);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //修改师门过期时间
    static boolean updateExpireTimeById(int shituId) {
        if (!exists(shituId)) {
            return false;
        }

        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu AS s SET s.expire_time = ADDDATE(s.expire_time, 30) WHERE s.id = ?");
            ps.setInt(1, shituId);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //查询师门成员
    static List<ShiTuCharacter> selectCharacterByState(int shituId, int state) {
        PreparedStatement ps;
        List<ShiTuCharacter> chs = new ArrayList<>();
        try {
            ps = ConnExt.getConn().prepareStatement("SELECT sc.*,\n" +
                    "       DATE_FORMAT(sc.join_time, '%Y-%m-%d %T') as jointime,\n" +
                    "       c.level\n" +
                    "FROM shitu_character AS sc,\n" +
                    "     characters as c\n" +
                    "WHERE c.id = sc.character_id\n" +
                    "  AND sc.shitu_id = ?\n" +
                    "  AND sc.state = ?\n" +
                    " ");
            ps.setInt(1, shituId);
            ps.setInt(2, state);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                chs.add(new ShiTuCharacter(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chs;
    }

    //查询师门成员
    static ShiTuCharacter selectCharacterByCharacterId(int characterId) {
        PreparedStatement ps;
        ShiTuCharacter sc = null;
        try {
            ps = ConnExt.getConn().prepareStatement("SELECT sc.*,\n" +
                    "       DATE_FORMAT(sc.join_time, '%Y-%m-d %T') as jointime,\n" +
                    "       c.level\n" +
                    "FROM shitu_character AS sc,\n" +
                    "     characters as c\n" +
                    "WHERE sc.character_id = ?");
            ps.setInt(1, characterId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                sc = new ShiTuCharacter(rs);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sc;
    }

    //查询师门成员
    static List<ShiTuCharacter> selectCharacterByShiTuId(int shituId) {
        PreparedStatement ps;
        List<ShiTuCharacter> chs = new ArrayList<>();
        try {
            ps = ConnExt.getConn().prepareStatement("SELECT sc.*, DATE_FORMAT(sc.join_time, '%Y-%m-d %T') as jointime, c.level\n" +
                    "FROM shitu_character AS sc,\n" +
                    "     characters as c\n" +
                    "WHERE sc.shitu_id = ?\n" +
                    "  and sc.character_id = c.id");
            ps.setInt(1, shituId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                chs.add(new ShiTuCharacter(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chs;
    }

    //删除师门
    static boolean deleteById(int shituId) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("DELETE s.* FROM shitu AS s WHERE s.id = ?");
            ps.setInt(1, shituId);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //删除师门成员
    static boolean deleteCharacterByShiTuId(int shituId) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("DELETE s.* FROM shitu_character AS s WHERE s.shitu_id = ?");
            ps.setInt(1, shituId);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //删除师门成员
    static boolean deleteCharacterByShiTuIdAndCharacterName(int shituId, String characterName) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("DELETE s.* FROM shitu_character AS s WHERE s.shitu_id = ? AND s.character_name = ?");
            ps.setInt(1, shituId);
            ps.setString(2, characterName);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //删除师门店铺
    static boolean deleteShopByShiTuId(int shituId) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("DELETE s.* FROM shitu_shop AS s WHERE s.shitu_id = ?");
            ps.setInt(1, shituId);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //查询师门店铺
    static List<ShiTuShop> selectShopByShiTuId(int shituId) {
        PreparedStatement ps;
        List<ShiTuShop> cs = new ArrayList<>();
        try {
            ps = ConnExt.getConn().prepareStatement("SELECT s.* FROM shitu_shop AS s WHERE s.shitu_id = ?");
            ps.setInt(1, shituId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cs.add(new ShiTuShop(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cs;
    }

    //查询师门店铺
    static ShiTuShop selectShopByShiTuIdAndItemId(int shituId, int itemId) {
        PreparedStatement ps;
        List<ShiTuShop> cs = new ArrayList<>();
        try {
            ps = ConnExt.getConn().prepareStatement("SELECT s.* FROM shitu_shop AS s WHERE s.shitu_id = ? AND s.item_id = ?");
            ps.setInt(1, shituId);
            ps.setInt(2, itemId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cs.add(new ShiTuShop(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cs.size() > 0 ? cs.get(0) : null;
    }

    //插入师门店铺
    static boolean insertShop(int shituId, int itemId, int itemCount, int itemLevel, int saleType, int salePrice) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("INSERT INTO shitu_shop(shitu_id,item_id,item_count,item_level,sale_type,sale_price) values (?,?,?,?,?,?)");
            ps.setInt(1, shituId);
            ps.setInt(2, itemId);
            ps.setInt(3, itemCount);
            ps.setInt(4, itemLevel);
            ps.setInt(5, saleType);
            ps.setInt(6, salePrice);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //增加师门店铺数量
    static boolean increaseItemCountById(int shituId, int itemId, int itemCount) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu_shop AS ss SET ss.item_count = ss.item_count + ? WHERE ss.shitu_id = ? AND ss.item_id = ?");
            ps.setInt(1, itemCount);
            ps.setInt(2, shituId);
            ps.setInt(3, itemId);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //减少师门店铺数量
    static boolean decreaseItemCountById(int shituId, int itemId, int itemCount) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu_shop AS ss SET ss.item_count = ss.item_count - ? WHERE ss.item_count >= ? AND ss.shitu_id = ? AND ss.item_id = ?");
            ps.setInt(1, itemCount);
            ps.setInt(2, itemCount);
            ps.setInt(3, shituId);
            ps.setInt(4, itemId);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //增加或减少师门贡献
    static boolean decreaseContribution(int shituId, int contribution) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu AS s SET s.contribution = s.contribution + ? WHERE s.id = ? AND (? < 0 AND s.contribution > ?)");
            ps.setInt(1, contribution);
            ps.setInt(2, shituId);
            ps.setInt(3, contribution);
            ps.setInt(4, contribution);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //增加或减少个人师门贡献
    static boolean decreaseCharacterContribution(int shituId, int characterId, int contribution) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu_character AS s SET s.contribution = s.contribution + ? WHERE s.shitu_id = ? AND s.character_id = ? AND (? < 0 AND s.contribution > ?)");
            ps.setInt(1, contribution);
            ps.setInt(2, shituId);
            ps.setInt(3, characterId);
            ps.setInt(4, contribution);
            ps.setInt(5, contribution);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //增加或减少个人师徒金
    static boolean decreaseCharacterMeso(int shituId, int characterId, int meso) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("UPDATE shitu_character AS s SET s.meso = s.meso + ? WHERE s.shitu_id = ? AND s.character_id = ? AND (? < 0 AND s.meso > ?)");
            ps.setInt(1, meso);
            ps.setInt(2, shituId);
            ps.setInt(3, characterId);
            ps.setInt(4, meso);
            ps.setInt(5, meso);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean insertShituRenwu(int shituId, int itemId, int shuliang) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("insert into shitu_renwu(shitu_id, item_id, shuliang, riqi) values (?,?,?,day(now()))");
            ps.setInt(1, shituId);
            ps.setInt(2, itemId);
            ps.setInt(3, shuliang);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean updateShituRenwu(int shituId, int renwuId, int shuliang) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("update shitu_renwu as t set t.leiji = t.leiji + ? where t.shitu_id = ? and t.renwu_id = ?");
            ps.setInt(1, shuliang);
            ps.setInt(2, shituId);
            ps.setInt(3, renwuId);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean deleteShituRenwu(int shituId, int renwuId) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("delete from shitu_renwu where shitu_id = ? and renwu_id = ?");
            ps.setInt(1, shituId);
            ps.setInt(2, renwuId);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static List<ShiTuRenwu> selectShituRenwu(int shituId) {
        PreparedStatement ps;
        List<ShiTuRenwu> ss = new ArrayList<>();
        try {
            ps = ConnExt.getConn().prepareStatement("select t.* from shitu_renwu as t where t.shitu_id = ?");
            ps.setInt(1, shituId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ss.add(new ShiTuRenwu(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ss;
    }

}
