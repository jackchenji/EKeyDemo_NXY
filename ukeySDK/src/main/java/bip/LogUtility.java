package bip;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 日志打印
 * Created by chenji on 2018/10/15.
 */
public class LogUtility
{
	private final static String TAG = "LogUtility";
	/**日志保存路径*/
	private static String LogUtility_SAVE_PATH ; //sd卡日志保存记录
	/**log开关*/
	private static final boolean LogUtility_SWITCH = true;

	private static String mylog;

	private static LogUtility _lg;
	
	//日志保存的数量 ,默认保存最近10天的日志
    public int LogUtilitySaveNum=10;

    static {
		LogUtility_SAVE_PATH= Environment.getExternalStorageDirectory().toString() + "/" + "BipLog/";
	}
	//single 
	public static LogUtility getInstance(){
		if(_lg == null){
			_lg = new LogUtility();
		}
		return _lg;
	}

	/**写日志到text中*/
	public void appendLog(String log){
		mylog=mylog+"\r\n"+log;
	}


	/**写日志到text中*/
	public void writeLogs(){

		if(LogUtility_SWITCH){
			File file = checkLogUtilityFileIsExist();
			if(file == null)
				return;
			FileOutputStream fos = null;
			try
			{
				fos = new FileOutputStream(file, true);
				fos.write((new Date().toLocaleString() + "	" + mylog).getBytes("gbk"));
				fos.write("\r\n".getBytes("gbk"));
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally{
				try
				{
					if(fos != null){
						fos.close();
						fos = null;
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				fos = null;
				file = null;
			}
		}
	}

	/**写日志到text中*/
	public void writeLog(String msg){

		if(msg == null)
			return;
		if(LogUtility_SWITCH){
			File file = checkLogUtilityFileIsExist();
			if(file == null)
				return;
			FileOutputStream fos = null;
			try
			{
				fos = new FileOutputStream(file, true);
				fos.write((new Date().toLocaleString() + "	" + msg).getBytes("gbk"));
				fos.write("\r\n".getBytes("gbk"));
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally{
				try
				{
					if(fos != null){
						fos.close();
						fos = null;
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				fos = null;
				file = null;
			}
		}
	}
	/**写日志到text中*/
	public void cleanLog(){
		mylog="";
		File file = new File(LogUtility_SAVE_PATH);
		if(!file.exists()){
			file.mkdirs();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String dateStr = sdf.format(new Date());
		dateStr="bipLog";
		file = new File(LogUtility_SAVE_PATH + dateStr + ".txt");
		if(isLogUtilityExist(file)) {
			file.delete();
		}
	}

	/**
	 * 堆栈打印日志信息
	 * @param e
	 * @return
	 */
	public void WriteStackTrace(Throwable e){
        if(e != null){
            //StringWriter sw = new StringWriter();
            //PrintWriter pw = new PrintWriter(sw);
            //e.printStackTrace(pw);
            //return sw.toString();
            writeLog(e.getStackTrace().toString());
        }
        //return "";
    }
	
	private LogUtility()
	{
		CheckLogUtilityDele();
		
	}
	
	private void CheckLogUtilityDele()
	{

		
	}
	
	
	/**判断文件路径是否存在*/
	private File checkLogUtilityFileIsExist(){
		//if(!MemorySpaceManager.isSDExist()){  //sd 锟斤拷锟角凤拷锟斤拷锟�
		//	return null;
		//}
		File file = new File(LogUtility_SAVE_PATH);
		if(!file.exists()){
			file.mkdirs();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String dateStr = sdf.format(new Date());
		dateStr="bipLog";
		file = new File(LogUtility_SAVE_PATH + dateStr + ".txt");
		if(!isLogUtilityExist(file)){
			try
			{
				file.createNewFile();
			}
			catch (IOException e)
			{
//				e.printStackTrace();

			}
		}else {
		   file.delete();
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		sdf = null;
		return file;
	}
	
	/**
	 * 判断文件是否存在
	 * @param file
	 * @return
	 */
	private boolean isLogUtilityExist(File file){
		boolean ret = false;
		try{
			File tempFile = new File(LogUtility_SAVE_PATH);
			File[] files = tempFile.listFiles();
			if(files==null)
				return ret;
			for(int i = 0; i < files.length; i++){
				String name = files[i].getName().trim();
				if(name!=null&&name.equalsIgnoreCase(file.getName())){
					ret = true;
					break;
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return ret;
	}
	

}