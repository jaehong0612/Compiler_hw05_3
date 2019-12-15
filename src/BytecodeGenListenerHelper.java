import java.util.Hashtable;

import generated.MiniCParser;
import generated.MiniCParser.ExprContext;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.If_stmtContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;


public class BytecodeGenListenerHelper {
	
	// <boolean functions>
	
	static boolean isFunDecl(MiniCParser.ProgramContext ctx, int i) {
		return ctx.getChild(i).getChild(0) instanceof MiniCParser.Fun_declContext;
	}
	
	// type_spec IDENT '[' ']'
	static boolean isArrayParamDecl(ParamContext param) {
		return param.getChildCount() == 4;
	}
	
	// global vars
	static int initVal(Var_declContext ctx) {
		return Integer.parseInt(ctx.LITERAL().getText());
	}

	// var_decl	: type_spec IDENT '=' LITERAL ';
	static boolean isDeclWithInit(Var_declContext ctx) {
		return ctx.getChildCount() == 5 ;
	}
	// var_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static boolean isArrayDecl(Var_declContext ctx) {
		return ctx.getChildCount() == 6;
	}

	// <local vars>
	// local_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static int initVal(Local_declContext ctx) {
		return Integer.parseInt(ctx.LITERAL().getText());
	}

	static boolean isArrayDecl(Local_declContext ctx) {
		return ctx.getChildCount() == 6;
	}
	
	static boolean isDeclWithInit(Local_declContext ctx) {
		return ctx.getChildCount() == 5 ;
	}
	
	static boolean isVoidF(Fun_declContext ctx) {
		if(getTypeText(ctx.type_spec()).equals("V")){
			return true;
		}
		return false;

	}
	
	static boolean isIntReturn(MiniCParser.Return_stmtContext ctx) {
		return ctx.getChildCount() ==3;
	}

	static boolean isVoidReturn(MiniCParser.Return_stmtContext ctx) {
		return ctx.getChildCount() == 2;
	}
	
	// <information extraction>
	static String getStackSize(Fun_declContext ctx) {
		return "32";
	}

	static String getLocalVarSize(Fun_declContext ctx) {
		return "32";
	}


	static String getTypeText(Type_specContext typespec) {
		String type = typespec.getText();
		if(type.equals("int")){
			return "I";
		}

		return "V";

	}
	// params
	static String getParamName(ParamContext param) {
		// <Fill in>
		String name = param.IDENT().getText();
		return name;
		// 매개변수의 이름을 getText로 받아준다.
	}

	static String getParamTypesText(ParamsContext params) {
		String typeText = "";

		for(int i = 0; i < params.param().size(); i++) {
			MiniCParser.Type_specContext typespec = (MiniCParser.Type_specContext)  params.param(i).getChild(0);
			typeText += getTypeText(typespec); // + ";";
		}
		return typeText;
	}

	static String getLocalVarName(Local_declContext local_decl) {
		// <Fill in>
		String name = local_decl.IDENT().getText();
		return name;
		// 로컬변수의 이름을 getText()를 이용하여 받아준다ㅏ.
	}

	static String getFunName(Fun_declContext ctx) {
		String name = ctx.IDENT().getText();
		return name;
	}
	
	static String getFunName(ExprContext ctx) {
		String Name = ctx.IDENT().getText();
		return Name;

	}
	
	static boolean noElse(If_stmtContext ctx) {
		return ctx.getChildCount() <= 5;
	}
	
	static String getFunProlog() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(".class public Test");
		stringBuffer.append(System.getProperty("line.separator"));
		stringBuffer.append(".super java/lang/Object");
		stringBuffer.append(System.getProperty("line.separator"));
		stringBuffer.append("; strandard initializer");
		stringBuffer.append(System.getProperty("line.separator"));
		stringBuffer.append(".method public <init>()V");
		stringBuffer.append(System.getProperty("line.separator"));
		stringBuffer.append("aload_0");
		stringBuffer.append(System.getProperty("line.separator"));
		stringBuffer.append("invokenonvirtual java/lang/Object/<init>()");
		stringBuffer.append(System.getProperty("line.separator"));
		/*stringBuffer.append("return");
		stringBuffer.append(System.getProperty("line.separator"));
		stringBuffer.append(".end method");
		stringBuffer.append(System.getProperty("line.separator"));
		stringBuffer.append(System.getProperty("line.separator"));*/
		return stringBuffer.toString();
		// return ".class public Test .....
		// ...
		// invokenonvirtual java/lang/Object/<init>()
		// return
		// .end method"

		// 프롤로그 바이트코드를 실행했을때 가장 먼저 실행되는 Object관련 함수의 내용을
		// 적어주고 이를 반환하게 했다.
	}
	
	static String getCurrentClassName() {
		return "Test";
	}
}
