package ms.login.mapper;

import java.util.*;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import ms.login.entity.Address;

public interface AddressMapper {
  class Sql {
    final static String TABLE = "address";
    final static String SELECT = "SELECT * FROM " + TABLE + " where uid = #{uid}";

    public static String addOrUpdate(Address address) {
      SQL sql = new SQL().INSERT_INTO(TABLE).VALUES("uid", "#{uid}");
      List<String> parts = new ArrayList<>();

      if (address.getDef() != Integer.MIN_VALUE) {
        sql.VALUES("def", "#{def}");
        parts.add("def = #{def}");
      }
      if (address.getAddress1() != null) {
        sql.VALUES("address1", "#{address1}");
        parts.add("address1 = #{address1}");
      }
      if (address.getAddress2() != null) {
        sql.VALUES("address2", "#{address2}");
        parts.add("address2 = #{address2}");
      }
      if (address.getAddress3() != null) {
        sql.VALUES("address3", "#{address3}");
        parts.add("address2 = #{address2}");        
      }
      if (address.getAddress4() != null) {
        sql.VALUES("address4", "#{address4}");
        parts.add("address2 = #{address2}");        
      }
      if (address.getAddress5() != null) {
        sql.VALUES("address5", "#{address5}");
        parts.add("address2 = #{address2}");        
      }
      if (address.getAddress6() != null) {
        sql.VALUES("address6", "#{address6}");
        parts.add("address2 = #{address2}");        
      }
      if (address.getAddress7() != null) {
        sql.VALUES("address7", "#{address7}");
        parts.add("address2 = #{address2}");        
      }
      if (address.getAddress8() != null) {
        sql.VALUES("address8", "#{address8}");
        parts.add("address2 = #{address2}");        
      }
      if (address.getAddress9() != null) {
        sql.VALUES("address9", "#{address9}");
        parts.add("address2 = #{address2}");        
      }

      return sql.toString() + " ON DUPLICATE KEY UPDATE " + String.join(",", parts);
    }

    public static String delete(Map<String, Object> param) {
      int id = (Integer) param.get("id");
      return "UPDATE " + TABLE + " SET address" + String.valueOf(id) +
        " = NULL WHERE uid = #{uid} AND def <> #{id}"; 
    }

    
  }

  @Select(Sql.SELECT)
  Address get(String uid);

  @InsertProvider(type = Sql.class, method = "addOrUpdate")
  int addOrUpdate(Address address);

  @UpdateProvider(type = Sql.class, method = "delete")
  int delete(@Param("uid") String uid, @Param("id") int id);
}
