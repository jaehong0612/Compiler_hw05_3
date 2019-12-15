import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import generated.MiniCParser;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;


public class SymbolTable {
	enum Type {
		INT, INTARRAY, VOID, ERROR
	}
	
	static public class VarInfo {
		Type type; 
		int id;
		int initVal;
		
		public VarInfo(Type type,  int id, int initVal) {
			this.type = type;
			this.id = id;
			this.initVal = initVal;
		}
		public VarInfo(Type type,  int id) {
			this.type = type;
			this.id = id;
			this.initVal = 0;
		}
	}
	
	static public class FInfo {
		public String sigStr;
	}
	
	private Map<String, VarInfo> _lsymtable = new HashMap<>();	// local v.
	private Map<String, VarInfo> _gsymtable = new HashMap<>();	// global v.
	private Map<String, FInfo> _fsymtable = new HashMap<>();	// function 
	
		
	private int _globalVarID = 0;
	private int _localVarID = 0;
	private int _labelID = 0;
	private int _tempVarID = 0;
	
	SymbolTable(){
		initFunDecl();
		initFunTable();
	}
	
	void initFunDecl(){		// at each func decl
		_lsymtable.clear();
		_localVarID = 0;
		_labelID = 0;
		_tempVarID = 32;

	}
	
	void putLocalVar(String varname, Type type){
		_lsymtable.put(varname,new VarInfo(type,_localVarID++));
		//<Fill here>
		// 매개변수로 받은 변수의 이름과 타입을 정보를 클래스로 만들어서 테이블에 넣는다.
		// 그리고 변수를 추가할때 id를 증가시킨다.
	}
	
	void putGlobalVar(String varname, Type type){
		_gsymtable.put(varname,new VarInfo(type,_globalVarID++));
		//<Fill here>
		// 위와 마찬가지로 매개변수로 받은 변수의 이름과 타입을 클래스로 만들어서 테이블에 넣는다.
	}
	
	void putLocalVarWithInitVal(String varname, Type type, int initVar){
		_lsymtable.put(varname,new VarInfo(type,_localVarID++,initVar));
		//<Fill here>
		// 위에서 정보클래스를 만들때 초기값까지 만들어서 저장한다.
	}
	void putGlobalVarWithInitVal(String varname, Type type, int initVar){
		_gsymtable.put(varname,new VarInfo(type,_globalVarID++,initVar));
		//<Fill here>
		// 위와 마찬가지로 진행한다.
	
	}
	void putParams(MiniCParser.ParamsContext params) {
		for(int i = 0; i < params.param().size(); i++) {
		//<Fill here>
			VarInfo varInfo;
					String type = BytecodeGenListenerHelper.getTypeText(params.param(i).type_spec());
			if(type.equals("I")){
				varInfo = new VarInfo(Type.INT, _localVarID++);
			}
			else{
				varInfo = new VarInfo(Type.VOID, _localVarID++);
			}
			_lsymtable.put(params.param(i).IDENT().getText(),varInfo);
		}
	}
	
	private void initFunTable() {
		FInfo printlninfo = new FInfo();
		printlninfo.sigStr = "java/io/PrintStream/println(I)V";
		
		FInfo maininfo = new FInfo();
		maininfo.sigStr = "main([Ljava/lang/String;)V";
		_fsymtable.put("_print", printlninfo);
		_fsymtable.put("main", maininfo);
	}
	
	public String getFunSpecStr(String fname) {
		FInfo fInfo = _fsymtable.get(fname);
		return fInfo.sigStr;
		// <Fill here>
		// 함수의 이름을 테이블에서 찾고 그 함수의 정보클래스가 가지고 있는 sigStr을 반환한다.
	}

	public String getFunSpecStr(Fun_declContext ctx) {
			// <Fill here>
			FInfo fInfo = _fsymtable.get(ctx.IDENT().getText());

			return fInfo.sigStr;
	} // ctx에서 함수의 이름을 빼낸뒤 이 정보를 가지고 테이블에서 조회하고
	  // 나온 정보클래스의 sigStr을 반환한다.
	
	public String putFunSpecStr(Fun_declContext ctx) {
		String fname = BytecodeGenListenerHelper.getFunName(ctx);
		String argtype = "";	
		String rtype = "";
		String res = "";
		
		// <Fill here>
		rtype += BytecodeGenListenerHelper.getTypeText((Type_specContext) ctx.getChild(0));
		// 바이트헬퍼의 getTypeText를 이용하여 반환 타입을 받아온다.
		argtype+= BytecodeGenListenerHelper.getParamTypesText(ctx.params());
        // 위와 마찬가지로 getParamTypeText를 이용하여 매개변수의 타입들을 모두 받아온다.
		res =  fname + "(" + argtype + ")" + rtype;

		FInfo finfo = new FInfo();
		finfo.sigStr = res;
		_fsymtable.put(fname, finfo);
		
		return res;
	}
	
	String getVarId(String name){
		// <Fill here>
		VarInfo varInfo = _lsymtable.get(name);
		if( varInfo == null){
			varInfo = _gsymtable.get(name);
			if(varInfo == null){
				return "";
			}
		}
		return String.valueOf(varInfo.id);
	} // 우선 로컬테이블에서 변수가 존재하는지 검색한뒤 없으면 글로벌테이블에 서 검색하고
	  // 검색한 결과를 반환한다.
	
	Type getVarType(String name){
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		if (lvar != null) {
			return lvar.type;
		}
		
		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		if (gvar != null) {
			return gvar.type;
		}
		
		return Type.ERROR;	
	}
	String newLabel() {
		return "label" + _labelID++;
	}
	
	String newTempVar() {
		String id = "";
		return id + _tempVarID--;
	}

	// global
	public String getVarId(Var_declContext ctx) {
		String sname = "";
		sname += getVarId(ctx.IDENT().getText());
		return sname;
		// <Fill here>	
	} // 밑에 함수와 똑같이 진행한다. ctx의 IDENT를 이용해 변수의 ID를 받고 이를 반환한다.

	// local
	public String getVarId(Local_declContext ctx) {
		String sname = "";
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}
	
}
