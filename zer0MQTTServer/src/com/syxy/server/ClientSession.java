package com.syxy.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;

import com.syxy.Aiohandler.AioReadHandler;
import com.syxy.Aiohandler.AioWriteHandler;
import com.syxy.protocol.CoderHandler;
import com.syxy.protocol.DecoderHandler;
import com.syxy.protocol.ProcessHandler;
import com.syxy.util.Log;

/**
 * <li>说明 客户session，每个连接一个ClientSession对象，用于处理客户请求和响应
 * <li>作者 zer0
 * <li>创建日期 2015-2-16
 */

public class ClientSession {

	// 定义编码处理器，业务处理器，解码处理器
	private CoderHandler coderHandler;// 编码处理器
	private DecoderHandler decoderHandler;// 解码处理器
	private ProcessHandler processHandler;// 业务处理器
	
	private TcpServer socketServer;
	private AsynchronousSocketChannel socketChannel;// 异步socket连结端
	private AioReadHandler readHandler;// 读取处理器
	private AioWriteHandler writeHandler;// 回写处理器	
	
	private ByteBuffer byteBuffer;// 缓冲区
	private ByteBuffer writeByteBuffer;// 回写缓冲区
	
	private Object index;// 客户端在索引
	
	public ClientSession(AsynchronousSocketChannel socketChannel, 
			AioReadHandler readHandler, 
			AioWriteHandler writeHandler,
			TcpServer socketServer) {
		this.socketChannel = socketChannel;
		this.readHandler = readHandler;
		this.writeHandler = writeHandler;
		this.socketServer = socketServer;
	}
	
	/**
	 * <li>方法名 registeHandler
	 * <li>@param coderHandler
	 * <li>@param decoderHandler
	 * <li>@param processHandler
	 * <li>返回类型 void
	 * <li>说明 把编码，解码和协议处理器注册给client，方便client读数据，写数据
	 *         的时候编码，解码
	 * <li>作者 zer0
	 * <li>创建日期 2015-2-20
	 */
	public void registeHandler(CoderHandler coderHandler, DecoderHandler decoderHandler, ProcessHandler processHandler){
		this.coderHandler = coderHandler;
		this.decoderHandler = decoderHandler;
		this.processHandler = processHandler;
	}
	
	/**
	 * <li>方法名 readEvent
	 * <li>返回类型 void
	 * <li>说明 读请求事件，调用AioReadHandler处理读事件
	 * <li>作者 zer0
	 * <li>创建日期 2015-2-21
	 */
	public void readEvent(){
		try {
			//如果socket通道未关闭，就读数据
			if (this.socketChannel.isOpen()){
				this.byteBuffer = ByteBuffer.allocate(1024 * 64);    
	            this.socketChannel.read(this.byteBuffer, this, this.readHandler);  
	        } else {  
	            Log.info("会话被取消或者关闭");  
	        }
		} catch (Exception e) {
			Log.warn(e.getMessage());
			this.close();
		}
	}
	
	/**
	 * <li>方法名 readRequest
	 * <li>返回类型 boolean
	 * <li>说明 读请求完成后，对得到的数据进行解码处理，以便能识别
	 * <li>作者 zer0
	 * <li>创建日期 2015-2-21
	 */
	public boolean readRequest(){
		Log.info("进行解码处理");	
		boolean returnValue = false;// 返回值	,若数据处理完毕无问题，改为true
								
		byteBuffer.flip();
		byteBuffer = this.decoderHandler.process(byteBuffer, this);// 解码处理
		
		returnValue = true;
		this.readEvent();
		
		return returnValue;
	}
	
	/**
	 * <li>方法名 process
	 * <li>返回类型 void
	 * <li>说明 处理读取的数据，处理完之后进行编码，通过AioWriteHandler回写到客户端
	 * <li>作者 zer0
	 * <li>创建日期 2015-2-22
	 */
	public void process(){
		try{
			this.processHandler.process(this);// 业务处理
			if(this.byteBuffer.get() > 0){// 不为空进行写出信息
				this.byteBuffer = this.coderHandler.process(byteBuffer, this);// 协议编码处理
				this.writeMessage(byteBuffer);// 回写数据
			}
		}catch(Exception e){
			e.printStackTrace();
			this.close();
		}
	}
	
	/**
	 * <li>方法名 processBlankRead
	 * <li>返回类型 void
	 * <li>说明 处理读取到的数据为空的情况
	 * <li>作者 zer0
	 * <li>创建日期 2015-2-22
	 */
	public void processBlankRead(){
		this.close();
	}
	
	/**
	 * <li>方法名 writeMessage
	 * <li>返回类型 void
	 * <li>说明 回写数据
	 * <li>作者 zer0
	 * <li>创建日期 2015-2-22
	 */
	public void writeMessage(ByteBuffer byteBuffer){		
		if(byteBuffer.get() > 0){
			this.socketChannel.write(this.byteBuffer, this, this.writeHandler);
		}
	}
	
	/**
	 * <li>方法名 getIp
	 * <li>返回类型 String
	 * <li>说明 获取客户端IP地址
	 * <li>作者 zer0
	 * <li>创建日期 2015-2-22
	 */
	public String getIp(){		
		try {
			return this.socketChannel.getRemoteAddress().toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * <li>方法名 close
	 * <li>返回类型 void
	 * <li>说明 关闭socket连接
	 * <li>作者 zer0
	 * <li>创建日期 2015-2-21
	 */
	public void close(){
		try{
			this.socketServer.closeServer(index);// 清除
			this.socketChannel.close();		
		} catch (ClosedChannelException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{			
			try{
				socketChannel.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}

	public Object getIndex() {
		return index;
	}

	public void setIndex(Object index) {
		this.index = index;
	}
	
}