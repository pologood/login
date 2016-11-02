package ms.login.mapper;

import java.util.*;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import commons.annotation.PartitionKey;
import ms.login.entity.Account;

public interface AccountMapper {
  class Sql {
    final static String TABLE = "account";

    final static String SELECT_BY_ID = "SELECT * FROM " + TABLE + " WHERE id = #{id}";
    final static String SELECT_BY_PHONE = "SELECT * FROM " + TABLE + " WHERE phone = #{phone}";
    final static String SELECT_BY_EMAIL = "SELECT * FROM " + TABLE + " WHERE email = #{email}";

    final static String UPDATE_PASSWORD_BY_ID =
      "UPDATE " + TABLE + " SET password = #{password} WHERE id = #{id}";
    final static String UPDATE_PASSWORD_BY_PHONE =
      "UPDATE " + TABLE + " SET password = #{password} WHERE phone = #{phone}";

    final static String GRANT_INCID_PERM = "UPDATE " + TABLE +
      " SET incId = #{incId}, perm = #{perm}" +
      " WHERE id = #{id} AND (incId < 0 OR incId = #{incId})";

    final static String REVOKE_INCID_PERM = "UPDATE " + TABLE +
      " SET incId = #{incId}, perm = #{perm} WHERE id = #{id}";

    final static String ADD_OPEN_USER = "INSERT INTO " + TABLE +
      " (id, email, password, name, headImg)" +
      " VALUES(#{id}, #{email}, ROUND(RAND() * 1000000), #{name}, #{headImg})" +
      " ON DUPLICATE KEY UPDATE name = #{name}, headImg = #{headImg}";

    public static String insert(Account account) {
      SQL sql = new SQL().INSERT_INTO(TABLE);
      if (account.getPhone() != null) {
        sql.VALUES("phone", "#{phone}");
      }
      if (account.getEmail() != null) {
        sql.VALUES("email", "#{email}");
      }
      if (account.getName() != null) {
        sql.VALUES("name", "#{name}");
      }
      sql.VALUES("password", "#{password}");
      sql.VALUES("incId", "#{incId}");
      sql.VALUES("perm", "#{perm}");
      sql.VALUES("status", "#{status}");
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

    public static String delete(Account account) {
      SQL sql = new SQL().UPDATE(TABLE);
      if (account.getPhone() != null) {
        sql.SET("phone = #{phone}");
      }
      if (account.getEmail() != null) {
        sql.SET("email = #{email}");
      }
      return sql.WHERE("id = #{id}").toString();        
    }
  }

  @Select(Sql.SELECT_BY_ID)
  Account find(long id);

  @Select(Sql.SELECT_BY_PHONE)
  Account findByPhone(String phone);

  @Select(Sql.SELECT_BY_EMAIL)
  Account findByEmail(String email);

  @InsertProvider(type = Sql.class, method = "insert")
  @Options(useGeneratedKeys=true, keyProperty = "id")
  int add(Account account);

  @Insert(Sql.ADD_OPEN_USER)
  int addOpenUser(@Param("id") long id, @Param("email") String email,
                  @Param("name") String name, @Param("headImg") String headImg);

  @UpdateProvider(type = Sql.class, method = "update")
  int update(Account account);

  @Update(Sql.UPDATE_PASSWORD_BY_ID)
  int updatePasswordById(@Param("id") long id, @Param("password") String password);

  @Update(Sql.UPDATE_PASSWORD_BY_PHONE)
  int updatePasswordByPhone(@Param("phone") String phone, @Param("password") String password);

  @Update(Sql.GRANT_INCID_PERM)
  int updateIncIdAndPerm(@Param("id") long id, @Param("incId") int incId,
                         @Param("perm") long perm);

  @Update(Sql.REVOKE_INCID_PERM)
  int revokeIncIdAndPerm(@Param("id") long id, @Param("incId") int incId,
                         @Param("perm") long perm);

  @UpdateProvider(type = Sql.class, method = "delete")
  int delete(Account account);
  
}
