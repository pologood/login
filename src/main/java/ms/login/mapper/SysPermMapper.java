package ms.login.mapper;

import java.util.*;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import ms.login.entity.SysPerm;

public interface SysPermMapper {
  class Sql {
    final static String TABLE = "sysPerm";
    final static String SELECT = "SELECT * FROM " + TABLE;

    public static String insert(SysPerm perm) {
      SQL sql = new SQL().INSERT_INTO(TABLE).VALUES("name", "#{name}");
      
      if (perm.getDesc() != null) {
        sql.VALUES("desc", "#{desc}");
      }

      return sql.toString();
    }

    public static String update(SysPerm perm) {
      SQL sql = new SQL().UPDATE(TABLE);
      if (perm.getName() != null) {
        sql.SET("name = #{name}");
      }
      if (perm.getDesc() != null) {
        sql.SET("desc = #{desc}");
      }
      if (perm.getStatus() != null) {
        sql.SET("status = #{status}");
      }
      return sql.WHERE("id = #{id}").toString();        
    }
  }

  @Select(Sql.SELECT)
  List<SysPerm> getAll();

  @InsertProvider(type = Sql.class, method = "insert")
  @Options(useGeneratedKeys=true, keyProperty = "id")
  int add(SysPerm perm);

  @UpdateProvider(type = Sql.class, method = "update")
  int update(SysPerm perm);
}
