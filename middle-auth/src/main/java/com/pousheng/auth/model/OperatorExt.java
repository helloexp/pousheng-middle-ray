package com.pousheng.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * 添加 realname
 * Created by will.gong on 2019/06/04
 */
public class OperatorExt implements Serializable {
	private static final long serialVersionUID = -1501078765118899286L;
	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.nonEmptyMapper().getMapper();
	private Long id;
	private Long userId;
	private String userName;
	private Long roleId;
	private String roleName;
	private Integer status;
	private Map<String, String> extra;
	@JsonIgnore
	private String extraJson;
	private Date createdAt;
	private Date updatedAt;
	private String realName;

	public boolean isActive() {
		return this.status != null && this.status == 1;
	}

	public void setExtra(Map<String, String> extra) {
		try {
			this.extra = extra;
			if (extra != null && !extra.isEmpty()) {
				this.extraJson = OBJECT_MAPPER.writeValueAsString(extra);
			} else {
				this.extraJson = null;
			}
		} catch (Exception e) {
			//ignore excetion
		}

	}

	public void setExtraJson(String extraJson) {
		try {
			this.extraJson = extraJson;
			if (Strings.isNullOrEmpty(extraJson)) {
				this.extra = Collections.emptyMap();
			} else {
				this.extra = (Map) OBJECT_MAPPER.readValue(extraJson, JacksonType.MAP_OF_STRING);
			}
		} catch (Exception e) {
			//ignore excetion
		} 
	}

	public Long getId() {
		return this.id;
	}

	public Long getUserId() {
		return this.userId;
	}

	public String getUserName() {
		return this.userName;
	}

	public Long getRoleId() {
		return this.roleId;
	}

	public String getRoleName() {
		return this.roleName;
	}

	public Integer getStatus() {
		return this.status;
	}

	public Map<String, String> getExtra() {
		return this.extra;
	}

	public String getExtraJson() {
		return this.extraJson;
	}

	public Date getCreatedAt() {
		return this.createdAt;
	}

	public Date getUpdatedAt() {
		return this.updatedAt;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (!(o instanceof OperatorExt)) {
			return false;
		} else {
			OperatorExt other = (OperatorExt) o;
			if (!other.canEqual(this)) {
				return false;
			} else {
				Object this$id = this.getId();
				Object other$id = other.getId();
				if (this$id == null) {
					if (other$id != null) {
						return false;
					}
				} else if (!this$id.equals(other$id)) {
					return false;
				}

				Object this$userId = this.getUserId();
				Object other$userId = other.getUserId();
				if (this$userId == null) {
					if (other$userId != null) {
						return false;
					}
				} else if (!this$userId.equals(other$userId)) {
					return false;
				}

				Object this$userName = this.getUserName();
				Object other$userName = other.getUserName();
				if (this$userName == null) {
					if (other$userName != null) {
						return false;
					}
				} else if (!this$userName.equals(other$userName)) {
					return false;
				}

				label110 : {
					Object this$roleId = this.getRoleId();
					Object other$roleId = other.getRoleId();
					if (this$roleId == null) {
						if (other$roleId == null) {
							break label110;
						}
					} else if (this$roleId.equals(other$roleId)) {
						break label110;
					}

					return false;
				}

				label103 : {
					Object this$roleName = this.getRoleName();
					Object other$roleName = other.getRoleName();
					if (this$roleName == null) {
						if (other$roleName == null) {
							break label103;
						}
					} else if (this$roleName.equals(other$roleName)) {
						break label103;
					}

					return false;
				}

				Object this$status = this.getStatus();
				Object other$status = other.getStatus();
				if (this$status == null) {
					if (other$status != null) {
						return false;
					}
				} else if (!this$status.equals(other$status)) {
					return false;
				}

				label89 : {
					Object this$extra = this.getExtra();
					Object other$extra = other.getExtra();
					if (this$extra == null) {
						if (other$extra == null) {
							break label89;
						}
					} else if (this$extra.equals(other$extra)) {
						break label89;
					}

					return false;
				}

				label82 : {
					Object this$extraJson = this.getExtraJson();
					Object other$extraJson = other.getExtraJson();
					if (this$extraJson == null) {
						if (other$extraJson == null) {
							break label82;
						}
					} else if (this$extraJson.equals(other$extraJson)) {
						break label82;
					}

					return false;
				}

				Object this$createdAt = this.getCreatedAt();
				Object other$createdAt = other.getCreatedAt();
				if (this$createdAt == null) {
					if (other$createdAt != null) {
						return false;
					}
				} else if (!this$createdAt.equals(other$createdAt)) {
					return false;
				}

				Object this$updatedAt = this.getUpdatedAt();
				Object other$updatedAt = other.getUpdatedAt();
				if (this$updatedAt == null) {
					if (other$updatedAt != null) {
						return false;
					}
				} else if (!this$updatedAt.equals(other$updatedAt)) {
					return false;
				}
				
				Object this$realName = this.getRealName();
				Object other$realName = other.getRealName();
				if (this$realName == null) {
					if (other$realName != null) {
						return false;
					}
				} else if (!this$realName.equals(other$realName)) {
					return false;
				}

				return true;
			}
		}
	}

	protected boolean canEqual(Object other) {
		return other instanceof OperatorExt;
	}

	public int hashCode() {
		int result = 1;
		Object $id = this.getId();
		result = result * 59 + ($id == null ? 43 : $id.hashCode());
		Object $userId = this.getUserId();
		result = result * 59 + ($userId == null ? 43 : $userId.hashCode());
		Object $userName = this.getUserName();
		result = result * 59 + ($userName == null ? 43 : $userName.hashCode());
		Object $roleId = this.getRoleId();
		result = result * 59 + ($roleId == null ? 43 : $roleId.hashCode());
		Object $roleName = this.getRoleName();
		result = result * 59 + ($roleName == null ? 43 : $roleName.hashCode());
		Object $status = this.getStatus();
		result = result * 59 + ($status == null ? 43 : $status.hashCode());
		Object $extra = this.getExtra();
		result = result * 59 + ($extra == null ? 43 : $extra.hashCode());
		Object $extraJson = this.getExtraJson();
		result = result * 59 + ($extraJson == null ? 43 : $extraJson.hashCode());
		Object $createdAt = this.getCreatedAt();
		result = result * 59 + ($createdAt == null ? 43 : $createdAt.hashCode());
		Object $updatedAt = this.getUpdatedAt();
		result = result * 59 + ($updatedAt == null ? 43 : $updatedAt.hashCode());
		Object $realName = this.getRealName();
		result = result * 59 + ($realName == null ? 43 : $realName.hashCode());
		return result;
	}

	public String toString() {
		return "OperatorExt(id=" + this.getId() + ", userId=" + this.getUserId() + ", userName=" + this.getUserName()
				+ ", realName=" + this.getRealName() + ", roleId=" + this.getRoleId() + ", roleName=" + this.getRoleName() 
				+ ", status=" + this.getStatus() + ", extra=" + this.getExtra() + ", extraJson=" + this.getExtraJson() 
				+ ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
	}
}
