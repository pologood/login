package ms.login.mapper;

import java.util.*;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import commons.annotation.PartitionKey;
import ms.login.entity.Account;

public interface AccountMapper {
  class Sql {
    final static String TABLE = "account";
    final static String SELECT_BY_PHONE = "SELECT * FROM " + TABLE + " WHERE phone = #{phone}";
    final static String SELECT_BY_EMAIL = "SELECT * FROM " + TABLE + " WHERE email = #{email}";

    public static String insert(Account account) {
      SQL sql = new SQL().INSERT_INTO(TABLE);
      if (account.getPhone() != null) {
        sql.VALUES("phone", "#{phone}");
      }
      if (account.getEmail() != null) {
        sql.VALUES("email", "#{email}");
      }
      sql.VALUES("password", "#{password}");
      return sql.toString();
    }

    public static String update(Account account) {
      SQL sql = new SQL().UPDATE(TABLE);
      if (account.getName() != null) {
        sql.SET("name = #{name}");
      }
      if (account.getHeadImg() != null) {
        sql.SET("headImg = #{headImg}");
      }
      if (account.getStatus() != null) {
        sql.SET("status = #{status}");
      }
      return sql.WHERE("id = #{id}").toString();        
    }
  }

  @Select(Sql.SELECT_BY_PHONE)
  Account findByPhone(String phone);

  @Select(Sql.SELECT_BY_EMAIL)
  Account findByEmail(String email);

  @InsertProvider(type = Sql.class, method = "insert")
  @Options(useGeneratedKeys=true, keyProperty = "id")
  int add(Account account);

  @UpdateProvider(type = Sql.class, method = "update")
  int update(Account employee);
}
