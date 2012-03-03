package com.momoonga.luarida.robotscript;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;

public class RobotScriptActivity extends Activity {
	private String	prInstallDataFilename = "install.data";
	private String	prTitle = "�^�C�g��";
	private String	prSampleFolder = "luarida";
	private String	prSampleFilename = "title.lua";
	private String	prSDCardDrive;
	private Timer	prTimer = null;   //�^�C�}���`
    private TextView prText;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        prText = (TextView)this.findViewById(R.id.text_view);

        //�^�C�g����lua�t�@�C������ǂݍ���
        if( getIniData()==false ){
    		//install.data����f�[�^���ǂݍ��߂Ȃ�������I��
    		prText.setText("install.data can not read.");
    		return;
        }
        prText.setText( prTitle + " Loading...");

    	//SD�J�[�h�̃Z�b�g�A�b�v
    	if( copyassetfile()==false){
    		//SD�J�[�h�����邩�ǂ����̃`�F�b�N����������I���
    		prText.setText("prTitle Loading...Error");
    		return;
    	}
    	//�^�C�}�[�ݒ���s��
    	SetLuaridaCallTimmer();
    }

	//**************************************************
    //�^�C�g����lua�t�@�C������ǂݍ���
	//**************************************************
	private boolean getIniData() {
		try {
			AssetManager as = getResources().getAssets();
			String[] fols = as.list("install");
			prSampleFolder = "luarida";

			if( fols.length==0 ){
				new AlertDialog.Builder(this)
				.setTitle("Luarida Folder Copy Error")
				.setMessage("�ǂݍ��ރt�@�C����������܂���B").show();
				return(false);
			}

			prSampleFolder = fols[0];
			InputStream is = as.open(prInstallDataFilename);
			InputStreamReader in =  new InputStreamReader(is, "SJIS");
			BufferedReader br = new BufferedReader(in);
			prTitle = br.readLine();
			prSampleFilename = br.readLine();
            br.close();
            in.close();
            is.close();
		} catch (IOException e) {
			e.printStackTrace();
			return( false );
		}
		return( true );
	}

	//**************************************************
    // asset�t�H���_��prSampleFolder�ȉ��ɂ��邷�ׂẴt�@�C����SD�J�[�h�ɃR�s�[���܂�
    //**************************************************
	private boolean copyassetfile() {

		String status = Environment.getExternalStorageState();
		if (status.equals(Environment.MEDIA_MOUNTED)) {
			//SD�J�[�h���}�E���g����Ă���Ƃ��́A���̖��̂�psSDCardDrive�ɓ����
			prSDCardDrive = Environment.getExternalStorageDirectory().getPath();
		}
		else{
			new AlertDialog.Builder(this)
			.setTitle("Luarida Folder Copy Error")
			.setMessage("SD�������J�[�h���}�E���g����Ă��܂���BSD�������J�[�h���ꂽ��A�N�����Ȃ����ĉ������B").show();
			return(false);
		}

	    // prSampleFolder�ȉ��̃t�@�C�������ׂăR�s�[����
		return( foldercopyall(prSampleFolder) );
	}

	//**************************************************
    // folder�ȉ��̃t�@�C�������ׂăR�s�[����
	//**************************************************
	private boolean foldercopyall(String folder) {
		Boolean blnRet = true;
		String[] files;
		AssetManager as = getResources().getAssets();

		try {
			File SDfolder = new File(prSDCardDrive + "/" + folder );
			//�t�H���_��������΁A�t�H���_�����܂�
			if( !SDfolder.exists()){
				SDfolder.mkdir();
			}
			else{
				//�����t�@�C���ł���΃G���[��Ԃ��܂�
				if( !SDfolder.isDirectory() ){
					return(false);
				}
			}
			files = as.list("install/" + folder);
			int DEFAULT_BUFFER_SIZE = 1024 * 4;
			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			InputStream is = null;

			for (String str : files) {
				if( str.equals("") ){	continue;	}
				try{
					is = as.open("install/"+folder+"/"+str);
				}
				catch (Exception e) {
					//�G���[�Ƃ������Ƃ́A�����ƁA�t�H���_�ɈႢ�Ȃ��B
//					Log.i("Error",e.toString());
					//�ċN�Ăяo��
					if( foldercopyall( folder+"/"+str )==false ){
						return( false );
					}
					is = null;
				}
				if( is==null ){	continue;	}
				//open�ł�����B
				String toFile = prSDCardDrive + "/" + folder + "/" + str;
				File outfile = new File(toFile);
				OutputStream output = new FileOutputStream(outfile);
				int n = 0;
				while (-1 != (n = is.read(buffer))) {
					output.write(buffer,0,n);
				}
				output.flush();
				output.close();
				is.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			blnRet = false;
		}
		return blnRet;
	}

	//**************************************************
    // �^�C�}��ݒ肷��
	//**************************************************
    private void SetLuaridaCallTimmer(){

    	//Timer��ݒ肷��
    	prTimer = new Timer(true);
    	final android.os.Handler handler = new android.os.Handler();
    	prTimer.schedule(
    			new TimerTask() {
    				@Override
    				public void run() {
    					handler.post( new Runnable(){
    						public void run(){

    							//�^�C�}�[���~�߂܂�
    							prTimer.cancel();

   								//Luarida���Ăяo��
   								startLuarida(prSDCardDrive+"/"+prSampleFilename);
    						}
        				});
        			}
        		}, 500, 500);  //����N���̒x���Ǝ����w��B�P�ʂ�ms
    }

	//**************************************************
    // Luarida���Ăяo���܂�
	//**************************************************
    public boolean startLuarida(String strText){
    	boolean retFlg = false;
		Intent intent = null;

    	try{
    		//strText = "file://" + strText;
    		//String luarida = "com.momoonga.luarida.LuaridaActivity";
    		//intent = new Intent(Intent.ACTION_VIEW);
			//int idx = luarida.lastIndexOf('.');
			//String pkg = luarida.substring(0, idx);
			//Uri data = Uri.parse(strText);
			//it.setClassName(pkg, luarida);
			//it.setDataAndType( data, "text/plain" );
			//it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			//startActivity(it);
			//retFlg = true;

    		strText = "file://" + strText;
			intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType( Uri.parse(strText), "x-luarida/lua" );
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			retFlg = true;
		}
		catch( Exception e)	{
			retFlg = false;
		}

		//�G���[���́A�}�[�P�b�g���Ăяo����Luarida�C���X�g�[���𑣂�
		if( retFlg==false ){

			AlertDialog dialog = null;
			//OK�{�^��
			dialog = new AlertDialog.Builder(this)
				.setIcon(R.drawable.ic_launcher)
				.setTitle("Luarida��������܂���")
				.setMessage(prTitle+"�����s����ɂ�Luarida���K�v�ł��B\nLuarida���}�[�P�b�g����C���X�g�[��������\n�ēx���s���ĉ������B")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				}).show();

			dialog.setOnDismissListener(new OnDismissListener(){
				@Override
				public void onDismiss(DialogInterface dialog) {
					Uri uri = Uri.parse("market://details?id=com.momoonga.luarida");
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(intent);
				}

			});
		}
		return retFlg;
    }

	//**************************************************
    // �V�X�e���������I�ɏI��点��
    //**************************************************
    public void ExitByForce(){
        //****** �����I�� *******
        System.exit(RESULT_OK);
    }

	@Override
	public void onPause(){
		super.onPause();		//�e���\�b�h��super.***�Ǝ������ŏ��ɌĂԂ̂����܂�
		finish();	//onDestroy()���Ă΂��B
	}


	@Override
	public void onStop(){
		super.onStop();		//�e���\�b�h��super.***�Ǝ������ŏ��ɌĂԂ̂����܂�
		finish();	//onDestroy()���Ă΂��B
	}

	@Override
	public void onDestroy(){
		super.onDestroy();		//�e���\�b�h��super.***�Ǝ������ŏ��ɌĂԂ̂����܂�
		//�I���������L�q

        // �V�X�e���������I�ɏI��点��
        ExitByForce();
    }
}