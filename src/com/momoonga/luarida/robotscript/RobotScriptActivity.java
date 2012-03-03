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
	private String	prTitle = "タイトル";
	private String	prSampleFolder = "luarida";
	private String	prSampleFilename = "title.lua";
	private String	prSDCardDrive;
	private Timer	prTimer = null;   //タイマを定義
    private TextView prText;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        prText = (TextView)this.findViewById(R.id.text_view);

        //タイトルやluaファイル名を読み込む
        if( getIniData()==false ){
    		//install.dataからデータが読み込めなかったら終了
    		prText.setText("install.data can not read.");
    		return;
        }
        prText.setText( prTitle + " Loading...");

    	//SDカードのセットアップ
    	if( copyassetfile()==false){
    		//SDカードがあるかどうかのチェック無かったら終わり
    		prText.setText("prTitle Loading...Error");
    		return;
    	}
    	//タイマー設定を行う
    	SetLuaridaCallTimmer();
    }

	//**************************************************
    //タイトルやluaファイル名を読み込む
	//**************************************************
	private boolean getIniData() {
		try {
			AssetManager as = getResources().getAssets();
			String[] fols = as.list("install");
			prSampleFolder = "luarida";

			if( fols.length==0 ){
				new AlertDialog.Builder(this)
				.setTitle("Luarida Folder Copy Error")
				.setMessage("読み込むファイルが見つかりません。").show();
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
    // assetフォルダのprSampleFolder以下にあるすべてのファイルをSDカードにコピーします
    //**************************************************
	private boolean copyassetfile() {

		String status = Environment.getExternalStorageState();
		if (status.equals(Environment.MEDIA_MOUNTED)) {
			//SDカードがマウントされているときは、その名称をpsSDCardDriveに入れる
			prSDCardDrive = Environment.getExternalStorageDirectory().getPath();
		}
		else{
			new AlertDialog.Builder(this)
			.setTitle("Luarida Folder Copy Error")
			.setMessage("SDメモリカードがマウントされていません。SDメモリカード入れた後、起動しなおして下さい。").show();
			return(false);
		}

	    // prSampleFolder以下のファイルをすべてコピーする
		return( foldercopyall(prSampleFolder) );
	}

	//**************************************************
    // folder以下のファイルをすべてコピーする
	//**************************************************
	private boolean foldercopyall(String folder) {
		Boolean blnRet = true;
		String[] files;
		AssetManager as = getResources().getAssets();

		try {
			File SDfolder = new File(prSDCardDrive + "/" + folder );
			//フォルダが無ければ、フォルダを作ります
			if( !SDfolder.exists()){
				SDfolder.mkdir();
			}
			else{
				//もしファイルであればエラーを返します
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
					//エラーということは、きっと、フォルダに違いない。
//					Log.i("Error",e.toString());
					//再起呼び出し
					if( foldercopyall( folder+"/"+str )==false ){
						return( false );
					}
					is = null;
				}
				if( is==null ){	continue;	}
				//openできたよ。
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
    // タイマを設定する
	//**************************************************
    private void SetLuaridaCallTimmer(){

    	//Timerを設定する
    	prTimer = new Timer(true);
    	final android.os.Handler handler = new android.os.Handler();
    	prTimer.schedule(
    			new TimerTask() {
    				@Override
    				public void run() {
    					handler.post( new Runnable(){
    						public void run(){

    							//タイマーを止めます
    							prTimer.cancel();

   								//Luaridaを呼び出す
   								startLuarida(prSDCardDrive+"/"+prSampleFilename);
    						}
        				});
        			}
        		}, 500, 500);  //初回起動の遅延と周期指定。単位はms
    }

	//**************************************************
    // Luaridaを呼び出します
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

		//エラー時は、マーケットを呼び出してLuaridaインストールを促す
		if( retFlg==false ){

			AlertDialog dialog = null;
			//OKボタン
			dialog = new AlertDialog.Builder(this)
				.setIcon(R.drawable.ic_launcher)
				.setTitle("Luaridaが見つかりません")
				.setMessage(prTitle+"を実行するにはLuaridaが必要です。\nLuaridaをマーケットからインストールした後\n再度実行して下さい。")
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
    // システムを強制的に終わらせる
    //**************************************************
    public void ExitByForce(){
        //****** 強制終了 *******
        System.exit(RESULT_OK);
    }

	@Override
	public void onPause(){
		super.onPause();		//各メソッドはsuper.***と自分を最初に呼ぶのが決まり
		finish();	//onDestroy()が呼ばれる。
	}


	@Override
	public void onStop(){
		super.onStop();		//各メソッドはsuper.***と自分を最初に呼ぶのが決まり
		finish();	//onDestroy()が呼ばれる。
	}

	@Override
	public void onDestroy(){
		super.onDestroy();		//各メソッドはsuper.***と自分を最初に呼ぶのが決まり
		//終了処理を記述

        // システムを強制的に終わらせる
        ExitByForce();
    }
}