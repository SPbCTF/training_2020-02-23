package ad.jtwigext;

import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;
import org.jtwig.value.context.ValueContext;
import org.jtwig.value.convert.string.StringConverter;

import java.util.Locale;

import ad.lang.Lang;

public class LangFunction extends SimpleJtwigFunction{
	// https://stackoverflow.com/questions/41614100/how-to-invoke-custom-functions-in-jtwig

	public LangFunction(){
	}

	@Override
	public String name(){
		return "L";
	}

	@Override
	public Object execute(FunctionRequest functionRequest){
		Locale locale=(Locale)functionRequest.getRenderContext().getCurrent(ValueContext.class).resolve("locale");
		String key=(String) functionRequest.get(0);
		if(functionRequest.getNumberOfArguments()==1){
			return Lang.Companion.getLangByLocale(locale).get(key);
		}else{
			String[] args=new String[functionRequest.getNumberOfArguments()-1];
			StringConverter conv=functionRequest.getEnvironment().getValueEnvironment().getStringConverter();
			for(int i=0;i<args.length;i++){
				args[i]=conv.convert(functionRequest.get(i+1));
			}
			return Lang.Companion.getLangByLocale(locale).get(key, args);
		}
	}
}
