package com.github.tools.shiro.realm;

import java.util.HashSet;
import java.util.List;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.bo.UserBO;
import com.github.enums.UserStatusEnum;
import com.github.po.User;
import com.github.service.UserService;
import com.github.shiro.util.CryptoUtil;

/**
 * 用户权限验证
 * 
 * @author jiangyf
 * @date 2017年7月31日 下午12:46:32
 */
public class UserRealm extends AuthorizingRealm {

	@Autowired
	private UserService userService;

	// 获取授权信息
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		String username = (String) principals.getPrimaryPrincipal();
		User user = getUser(username);
		List<String> roles = userService.listRole(user.getId());
		List<String> permissions = userService.listPermission(user.getId());
		SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
		authorizationInfo.setRoles(new HashSet<>(roles));
		authorizationInfo.setStringPermissions(new HashSet<>(permissions));
		return authorizationInfo;
	}

	// 获取身份验证信息
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
			throws AuthenticationException {
		String username = (String) token.getPrincipal();
		String password = new String((char[]) token.getCredentials());
		User user = getUser(username);
		// 校验密码，可以交给AuthenticatingRealm使用CredentialsMatcher进行密码匹配
		if (!CryptoUtil.encryptPassword(password, user.getSalt()).equals(user.getPassword())) {
			throw new IncorrectCredentialsException();
		}
		return new SimpleAuthenticationInfo(username, password, ByteSource.Util.bytes(user.getSalt()),
				getName());
	}

	/**
	 * 获取用户信息
	 * 
	 * @param username
	 *            用户名
	 * @return
	 */
	private User getUser(String username) {
		User user = userService.getUser(UserBO.of(null, username, null));
		if (user == null) {
			throw new UnknownAccountException();
		}
		if (UserStatusEnum.LOCKED.getCode() == user.getStatus()) {
			throw new LockedAccountException();
		}
		return user;
	}

}
