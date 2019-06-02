package com.ruoyi.web.controller.system;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.ExcelUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.shiro.service.PasswordService;
import com.ruoyi.framework.util.ShiroUtils;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.system.domain.SysPost;
import com.ruoyi.system.domain.SysUser;
import com.ruoyi.system.domain.SysUserDeptPost;
import com.ruoyi.system.domain.SysUserPost;
import com.ruoyi.system.service.ISysPostService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserDeptPostService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.web.core.base.BaseController;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户信息
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/system/user")
public class SysUserController extends BaseController
{
    private String prefix = "system/user";

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysPostService postService;

    @Autowired
    private ISysUserDeptPostService sysUserDeptPostService;

    @Autowired
    private PasswordService passwordService;

    @RequiresPermissions("system:user:view")
    @GetMapping()
    public String user()
    {
        return prefix + "/user";
    }

    @RequiresPermissions("system:user:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:user:export")
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(SysUser user)
    {
        List<SysUser> list = userService.selectUserList(user);
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        return util.exportExcel(list, "user");
    }

    /**
     * 新增用户
     */
    @GetMapping("/add")
    public String add(ModelMap mmap)
    {
        mmap.put("roles", roleService.selectRoleAll());
        mmap.put("posts", postService.selectPostAll());
        return prefix + "/add";
    }

    /**
     * 新增保存用户
     */
    @RequiresPermissions("system:user:add")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @Transactional(rollbackFor = Exception.class)
    @ResponseBody
    public AjaxResult addSave(SysUser user,String managementIds)
    {
        if (StringUtils.isNotNull(user.getUserId()) && SysUser.isAdmin(user.getUserId()))
        {
            return error("不允许修改超级管理员用户");
        }
        user.setSalt(ShiroUtils.randomSalt());
        user.setPassword(passwordService.encryptPassword(user.getLoginName(), user.getPassword(), user.getSalt()));
        user.setCreateBy(ShiroUtils.getLoginName());
        return toAjax(userService.insertUser(user,managementIds));
    }

    /**
     * 修改用户
     */
    @GetMapping("/edit/{userId}")
    public String edit(@PathVariable("userId") Long userId, ModelMap mmap)
    {
        SysUser user = userService.selectUserById(userId);
        if(user.getRoles().isEmpty()){
            user.setRoles(null);
        }
        mmap.put("user", user);
        mmap.put("roles", roleService.selectRolesByUserId(userId));
        List<SysPost>  posts = postService.selectPostsByUserId(userId);
        mmap.put("posts",posts);

       List<SysUserPost>   userPost = userService.selectPostId(userId);
        if(userPost != null){
         for (SysUserPost post : userPost) {
            mmap.put("postId",post.getPostId());
            }
        }
        return prefix + "/edit";
    }

    /**
     * 修改保存用户
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @Transactional(rollbackFor = Exception.class)
    @ResponseBody
    public AjaxResult editSave(SysUser user,String managementIds,String role)
    {
        if (StringUtils.isNotNull(user.getUserId()) && SysUser.isAdmin(user.getUserId()))
        {
            return error("不允许修改超级管理员用户");
        }
        user.setUpdateBy(ShiroUtils.getLoginName());

        return toAjax(userService.updateUser(user,managementIds));
    }
    /** 查看 所管理的部门 */
    @GetMapping("/selectUserDepts/{userId}")
    public String  selectUserDepts(@PathVariable("userId") Long userId,ModelMap modelMap)
    {
        StringBuffer sb = new StringBuffer();

        List<SysUserDeptPost> list = sysUserDeptPostService.selectUserDepts(userId);
        if(list.size()>0){
            for (SysUserDeptPost userDeptPost : list) {
                sb.append(userDeptPost.getDeptName()+",");
            }
        }
        modelMap.put("userId",userId);
        modelMap.put("sb",sb);
        return prefix + "/editDepts";
    }

    /** 修改 所管理部门 */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PostMapping("/updateDepts")
    @Transactional(rollbackFor = Exception.class)
    @ResponseBody
    public AjaxResult updateDepts(String deptIds,Long userId)
    {
        return toAjax(sysUserDeptPostService.updateDepts(deptIds,userId));
    }


    /**
     * 离职员工
     */
    @RequiresPermissions("system:user:departure")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @GetMapping("/departure/{userId}/{status}")
    @Transactional(rollbackFor = Exception.class)
    @ResponseBody
    public AjaxResult  departureUser(@PathVariable("userId") Long userId,@PathVariable("status") String status)
    {
        return toAjax(userService.departureUser(userId,status));
    }

    /**
     * 员工不参与积分
     */
    @RequiresPermissions("system:user:integral")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @GetMapping("/integral/{userId}/{integralStatus}")
    @Transactional(rollbackFor = Exception.class)
    @ResponseBody
    public AjaxResult  integralUser(@PathVariable("userId") Long userId,@PathVariable("integralStatus") String integralStatus)
    {
        return toAjax(userService.integralUser(userId,integralStatus));
    }

    @RequiresPermissions("system:user:resetPwd")
    @Log(title = "重置密码", businessType = BusinessType.UPDATE)
    @GetMapping("/resetPwd/{userId}")
    public String resetPwd(@PathVariable("userId") Long userId, ModelMap mmap)
    {
        mmap.put("user", userService.selectUserById(userId));
        return prefix + "/resetPwd";
    }

    @RequiresPermissions("system:user:resetPwd")
    @Log(title = "重置密码", businessType = BusinessType.UPDATE)
    @PostMapping("/resetPwd")
    @ResponseBody
    public AjaxResult resetPwdSave(SysUser user)
    {
        user.setSalt(ShiroUtils.randomSalt());
        user.setPassword(passwordService.encryptPassword(user.getLoginName(), user.getPassword(), user.getSalt()));
        return toAjax(userService.resetUserPwd(user));
    }

    @RequiresPermissions("system:user:remove")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        try
        {
            return toAjax(userService.deleteUserByIds(ids));
        }
        catch (Exception e)
        {
            return error(e.getMessage());
        }
    }

    /**
     * 校验用户名
     */
    @PostMapping("/checkLoginNameUnique")
    @ResponseBody
    public String checkLoginNameUnique(SysUser user)
    {
        return userService.checkLoginNameUnique(user.getLoginName());
    }

    /**
     * 校验手机号码
     */
    @PostMapping("/checkPhoneUnique")
    @ResponseBody
    public String checkPhoneUnique(SysUser user)
    {
        return userService.checkPhoneUnique(user);
    }

    /**
     * 校验email邮箱
     */
    @PostMapping("/checkEmailUnique")
    @ResponseBody
    public String checkEmailUnique(SysUser user)
    {
        return userService.checkEmailUnique(user);
    }

    /**
     *  设置 普通审批人和高级审批人
     */
    @RequiresPermissions("system:user:resetPwd")
    @Log(title = "设置 普通审批人和高级审批人", businessType = BusinessType.OTHER)
    @GetMapping("/sysApp/{userId}/{isApprover}")
    @ResponseBody
    public AjaxResult sysApp(@PathVariable("userId") Long userId,@PathVariable("isApprover") String isApprover )
    {
        return toAjax(userService.sysAppPel(userId,isApprover));
    }
    /**
     *  设置 积分申诉人
     */
    @RequiresPermissions("system:user:resetPwd")
    @Log(title = "设置 积分申诉人", businessType = BusinessType.OTHER)
    @GetMapping("/sysComplainant/{userId}/{integralComplainant}")
    @ResponseBody
    public AjaxResult sysComplainant(@PathVariable("userId") Long userId,@PathVariable("integralComplainant") String integralComplainant )
    {
        return toAjax(userService.sysComplainant(userId,integralComplainant));
    }


}