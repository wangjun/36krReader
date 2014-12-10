package com.yanshi.my36kr.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.UploadFileListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.yanshi.my36kr.R;
import com.yanshi.my36kr.bean.Constant;
import com.yanshi.my36kr.bean.bmob.User;
import com.yanshi.my36kr.biz.UserProxy;
import com.yanshi.my36kr.ui.base.BaseActivity;
import com.yanshi.my36kr.utils.SDCardUtils;
import com.yanshi.my36kr.utils.StringUtils;
import com.yanshi.my36kr.utils.ToastFactory;
import com.yanshi.my36kr.view.dialog.ConfirmDialogFragment;
import com.yanshi.my36kr.view.dialog.EditTextDialogFragment;
import com.yanshi.my36kr.view.dialog.ListDialogFragment;
import com.yanshi.my36kr.view.dialog.LoadingDialogFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

/**
 * 个人中心页面
 * 作者：yanshi
 * 时间：2014-11-28 15:43
 */
public class PersonalActivity extends BaseActivity {

    private static final int REQUEST_CODE_LOGIN = 0x1000;
    private static final int REQUEST_CODE_ALBUM = 0x1001;
    private static final int REQUEST_CODE_CAMERA = 0x1002;
    private static final int REQUEST_CODE_CROP_IMG = 0x1003;
    Uri imageUri;//存放头像的uri

    ImageView userAvatarIv;//用户头像
    TextView userNicknameTv, userSexTv, userSignatureTv;//昵称、性别、个性签名
    Button userAvatarBtn, userNicknameBtn, userSexBtn, userSignatureBtn, myFavoriteBtn;
    Button userLogoutBtn;//退出账号按钮
    LoadingDialogFragment loadingDialogFragment;

    User user;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.personal);
        initView();
        initListener();

        if (!UserProxy.isLogin(this)) {
            ToastFactory.getToast(this, getResources().getString(R.string.personal_login_first)).show();
            jumpToActivityForResult(this, LoginActivity.class, REQUEST_CODE_LOGIN, null);
            return;
        }
        user = UserProxy.getCurrentUser(this);
        if (null != user) {
            setUserInfo(user);
        }
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            EditTextDialogFragment editTextDialogFragment = new EditTextDialogFragment();
            switch (v.getId()) {
                case R.id.personal_my_favorite_btn://我的收藏
                    jumpToActivity(PersonalActivity.this, MyFavoriteActivity.class, null);
                    break;
                case R.id.personal_user_logout_btn://退出登录
                    String title = getResources().getString(R.string.confirm_dialog_title, "退出登录");
                    ConfirmDialogFragment confirmDialogFragment = new ConfirmDialogFragment();
                    confirmDialogFragment.setParams(title, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            UserProxy.logout(PersonalActivity.this);
                            PersonalActivity.this.finish();
                        }
                    }, null);
                    confirmDialogFragment.show(PersonalActivity.this.getFragmentManager(), "personal_confirm_dialog");
                    break;
                case R.id.personal_user_avatar_btn://头像
                    showAvatarDialog();
                    break;
                case R.id.personal_user_nickname_btn://昵称
                    editTextDialogFragment.setEditTextParams(userNicknameTv.getText().toString(), true, 20);
                    editTextDialogFragment.setMyOnClickListener(new EditTextDialogFragment.MyOnClickListener() {
                        @Override
                        public void onClick(String str) {
                            if (null == str || StringUtils.isBlank(str)) return;
                            loadingDialogFragment.show(getFragmentManager(), "set_nickname_loading_dialog");
                            UserProxy.updateUserInfo(mContext, user, str, null, null, new UserProxy.UserUpdateListener() {
                                @Override
                                public void onSuccess() {
                                    loadingDialogFragment.dismiss();
                                    ToastFactory.getToast(mContext, getResources().getString(R.string.personal_update_success)).show();
                                    userNicknameTv.setText(user.getNickname());
                                }

                                @Override
                                public void onFailure(String msg) {
                                    loadingDialogFragment.dismiss();
                                    ToastFactory.getToast(mContext, getResources().getString(R.string.personal_update_failed) + msg).show();
                                }
                            });
                        }
                    });
                    editTextDialogFragment.show(getFragmentManager(), "set_nickname_list_dialog");
                    break;
                case R.id.personal_user_sex_btn://性别
                    final String[] str = {"男", "女"};
                    ListDialogFragment listDialogFragment = new ListDialogFragment();
                    listDialogFragment.setParams(null, str, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loadingDialogFragment.show(getFragmentManager(), "set_sex_loading_dialog");
                            UserProxy.updateUserInfo(mContext, user, null, str[which], null, new UserProxy.UserUpdateListener() {
                                @Override
                                public void onSuccess() {
                                    loadingDialogFragment.dismiss();
                                    ToastFactory.getToast(mContext, getResources().getString(R.string.personal_update_success)).show();
                                    userSexTv.setText(user.getSex());
                                }

                                @Override
                                public void onFailure(String msg) {
                                    loadingDialogFragment.dismiss();
                                    ToastFactory.getToast(mContext, getResources().getString(R.string.personal_update_failed) + msg).show();
                                }
                            });
                        }
                    });
                    listDialogFragment.show(getSupportFragmentManager(), "set_sex_list_dialog");
                    break;
                case R.id.personal_user_signature_btn://个性签名
                    editTextDialogFragment.setEditTextParams(userSignatureTv.getText().toString(), false, 100);
                    editTextDialogFragment.setMyOnClickListener(new EditTextDialogFragment.MyOnClickListener() {
                        @Override
                        public void onClick(String str) {
                            if (null == str || StringUtils.isBlank(str)) return;
                            loadingDialogFragment.show(getFragmentManager(), "set_signature_loading_dialog");
                            UserProxy.updateUserInfo(mContext, user, null, null, str, new UserProxy.UserUpdateListener() {
                                @Override
                                public void onSuccess() {
                                    loadingDialogFragment.dismiss();
                                    ToastFactory.getToast(mContext, getResources().getString(R.string.personal_update_success)).show();
                                    userSignatureTv.setText(user.getSignature());
                                }

                                @Override
                                public void onFailure(String msg) {
                                    loadingDialogFragment.dismiss();
                                    ToastFactory.getToast(mContext, getResources().getString(R.string.personal_update_failed) + msg).show();
                                }
                            });
                        }
                    });
                    editTextDialogFragment.show(getFragmentManager(), "set_signature_list_dialog");
                    break;
            }
        }
    };

    private void initListener() {
        myFavoriteBtn.setOnClickListener(mOnClickListener);
        userLogoutBtn.setOnClickListener(mOnClickListener);
        userAvatarBtn.setOnClickListener(mOnClickListener);
        userNicknameBtn.setOnClickListener(mOnClickListener);
        userSexBtn.setOnClickListener(mOnClickListener);
        userSignatureBtn.setOnClickListener(mOnClickListener);
    }

    private void initView() {
        userAvatarIv = (ImageView) findViewById(R.id.personal_user_avatar_iv);
        userNicknameTv = (TextView) findViewById(R.id.personal_user_nickname_tv);
        userSexTv = (TextView) findViewById(R.id.personal_user_sex_tv);
        userSignatureTv = (TextView) findViewById(R.id.personal_user_signature_tv);
        userAvatarBtn = (Button) findViewById(R.id.personal_user_avatar_btn);
        userNicknameBtn = (Button) findViewById(R.id.personal_user_nickname_btn);
        userSexBtn = (Button) findViewById(R.id.personal_user_sex_btn);
        userSignatureBtn = (Button) findViewById(R.id.personal_user_signature_btn);
        myFavoriteBtn = (Button) findViewById(R.id.personal_my_favorite_btn);
        userLogoutBtn = (Button) findViewById(R.id.personal_user_logout_btn);

        loadingDialogFragment = new LoadingDialogFragment();
        loadingDialogFragment.setParams(getResources().getString(R.string.loading_dialog_title));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (RESULT_OK == resultCode) {
            switch (requestCode) {
                case REQUEST_CODE_LOGIN://登录成功
                    user = UserProxy.getCurrentUser(this);
                    if (null != user) setUserInfo(user);
                    break;
                case REQUEST_CODE_ALBUM://相册照片选好了
                    if (null != data && null != imageUri) {
//                        Bitmap bitmap = decodeUriAsBitmap(imageUri);//decode bitmap
//                        userAvatarIv.setImageBitmap(bitmap);
                        Log.d(Constant.TAG, "imgUri--->" + imageUri.toString());
                        ImageLoader.getInstance().displayImage(imageUri.toString(), userAvatarIv, mMyApplication.getAvatarOptions());
                        uploadAvatar(imageUri.getPath());
                    }
                    break;
                case REQUEST_CODE_CAMERA://照片拍好了
                    if (null != imageUri) {
                        cropImageUri(imageUri);
                    }
                    break;
                case REQUEST_CODE_CROP_IMG://拍好的照片裁剪好了
                    if (null != data && null != imageUri) {
                        Log.d(Constant.TAG, "imgUri--->" + imageUri.toString());
                        ImageLoader.getInstance().displayImage(imageUri.toString(), userAvatarIv, mMyApplication.getAvatarOptions());
                        uploadAvatar(imageUri.getPath());
                    }
                    break;
            }
        } else {
            if (!UserProxy.isLogin(this)) this.finish();
        }
    }

    /**
     * 设置登录用户的信息
     */
    private void setUserInfo(User user) {
        //头像
        String imgUrl;
        if (null != user.getAvatar() && null != (imgUrl = user.getAvatar().getFileUrl())) {
            ImageLoader.getInstance().displayImage(imgUrl, userAvatarIv, mMyApplication.getOptions(R.drawable.ic_user_avatar));
        }
        //昵称
        userNicknameTv.setText(user.getNickname());
        //性别
        userSexTv.setText(user.getSex());
        //个性签名
        userSignatureTv.setText(user.getSignature());
    }

    /**
     * 点击头像弹出的dialog
     */
    private void showAvatarDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
        alertDialog.setContentView(R.layout.personal_avatar_dialog_view);

        Button cameraBtn = (Button) alertDialog.findViewById(R.id.personal_avatar_dialog_view_camera_btn);
        Button albumBtn = (Button) alertDialog.findViewById(R.id.personal_avatar_dialog_view_album_btn);
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                takePhotoFromCamera();
            }
        });
        albumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                getAvatarFromAlbum();
            }
        });
    }

    /**
     * 去拍照
     */
    private void takePhotoFromCamera() {
        imageUri = Uri.fromFile(getCameraAvatarFile());

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }

    /**
     * 去相册选择照片
     */
    private void getAvatarFromAlbum() {
        imageUri = Uri.fromFile(getAlbumAvatarFile());
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 150);
        intent.putExtra("outputY", 150);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true); // no face detection
        startActivityForResult(intent, REQUEST_CODE_ALBUM);
    }

    /**
     * 去裁剪图片（照片拍完后）
     *
     * @param uri
     */
    private void cropImageUri(Uri uri) {
        if (null == uri) return;
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true); // no face detection
        startActivityForResult(intent, REQUEST_CODE_CROP_IMG);
    }

    /**
     * 返回暂存头像图片的file（从相册选取照片时）
     * @return
     */
    public File getAlbumAvatarFile() {
        return new File(getExternalCacheDir(), "user_icon.jpg");
    }

    /**
     * 返回拍摄的图片的file
     * @return
     */
    public File getCameraAvatarFile() {
        File file = new File(SDCardUtils.getAlbumStorageDir("36kr!"),
                "user_icon_" + new Date().getTime() + ".jpg");
        Log.d(Constant.TAG, "Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)--->\n" + file.getAbsolutePath());
        return file;
    }

    /**
     * 解析uri返回bitmap
     *
     * @param uri
     * @return
     */
    private Bitmap decodeUriAsBitmap(Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    /**
     * 上传头像到Bmob后台
     *
     * @param avatarPath
     */
    private void uploadAvatar(String avatarPath) {
        if (avatarPath == null || StringUtils.isBlank(avatarPath)) return;
        if (loadingDialogFragment != null) {
            loadingDialogFragment.show(getFragmentManager(), "upload_avatar_loading_dialog");
        }
        final BmobFile bmobFile = new BmobFile(new File(avatarPath));
        bmobFile.upload(mContext, new UploadFileListener() {
            @Override
            public void onSuccess() {
                ToastFactory.getToast(mContext, getResources().getString(R.string.personal_upload_avatar_success)).show();
                UserProxy.updateUserAvatar(mContext, user, bmobFile, new UserProxy.UserUpdateListener() {
                    @Override
                    public void onSuccess() {
                        if (loadingDialogFragment != null) loadingDialogFragment.dismiss();
                        ToastFactory.getToast(mContext, getResources().getString(R.string.personal_update_success)).show();
                    }

                    @Override
                    public void onFailure(String msg) {
                        if (loadingDialogFragment != null) loadingDialogFragment.dismiss();
                        ToastFactory.getToast(mContext, getResources().getString(R.string.personal_update_failed) + msg).show();
                    }
                });
            }

            @Override
            public void onProgress(Integer progress) {
            }

            @Override
            public void onFailure(int code, String msg) {
                if (loadingDialogFragment != null) loadingDialogFragment.dismiss();
                ToastFactory.getToast(mContext, getResources().getString(R.string.personal_upload_avatar_failed) + msg).show();
            }
        });
    }
}