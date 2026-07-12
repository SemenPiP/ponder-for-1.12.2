package net.createmod.catnip.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public abstract class ConfigBase {
    @Nullable public Configuration specification;
    protected int depth;
    protected final List<CValue<?>> allValues = new ArrayList<CValue<?>>();
    protected final List<ConfigBase> children = new ArrayList<ConfigBase>();
    private final List<String> groups = new ArrayList<String>();
    private String bindingPrefix;

    public abstract String getName();

    public synchronized void registerAll(Configuration configuration) {
        if (configuration == null) throw new NullPointerException("configuration");
        specification = configuration;
        configuration.load();
        bind(configuration, getName());
        if (configuration.hasChanged()) configuration.save();
        onLoad();
    }

    private void bind(Configuration configuration, String prefix) {
        specification = configuration;
        bindingPrefix = prefix;
        for (CValue<?> value : allValues) value.register(configuration, prefix);
        for (ConfigBase child : children) child.bind(configuration, prefix + "." + child.getName());
    }

    public synchronized void reload() {
        if (specification == null) throw new IllegalStateException("Config " + getName() + " is not registered");
        specification.load();
        bind(specification, getName());
        onReload();
    }
    public synchronized void save() { if (specification != null && specification.hasChanged()) specification.save(); }
    public void onLoad() { for (ConfigBase child : children) child.onLoad(); }
    public void onReload() { for (ConfigBase child : children) child.onReload(); }

    protected ConfigBool b(boolean current,String name,String... comment){return new ConfigBool(name,current,comment);}
    protected ConfigFloat f(float current,float min,float max,String name,String... comment){return new ConfigFloat(name,current,min,max,comment);}
    protected ConfigFloat f(float current,float min,String name,String... comment){return f(current,min,Float.MAX_VALUE,name,comment);}
    protected ConfigInt i(int current,int min,int max,String name,String... comment){return new ConfigInt(name,current,min,max,comment);}
    protected ConfigInt i(int current,int min,String name,String... comment){return i(current,min,Integer.MAX_VALUE,name,comment);}
    protected ConfigInt i(int current,String name,String... comment){return i(current,Integer.MIN_VALUE,Integer.MAX_VALUE,name,comment);}
    protected ConfigString s(String current,String name,String... comment){return new ConfigString(name,current,comment);}
    protected <T extends Enum<T>> ConfigEnum<T> e(T current,String name,String... comment){return new ConfigEnum<T>(name,current,comment);}
    protected ConfigGroup group(int groupDepth,String name,String... comment){return new ConfigGroup(name,groupDepth,comment);}
    protected <T extends ConfigBase> T nested(int groupDepth,Supplier<T> constructor,String... comment){
        T config=constructor.get();new ConfigGroup(config.getName(),groupDepth,comment);children.add(config);return config;
    }
    public List<CValue<?>> getAllValues(){List<CValue<?>> result=new ArrayList<CValue<?>>(allValues);for(ConfigBase child:children)result.addAll(child.getAllValues());return Collections.unmodifiableList(result);}
    public List<ConfigBase> getChildren(){return Collections.unmodifiableList(new ArrayList<ConfigBase>(children));}
    @Nullable public CValue<?> find(String category,String key){for(CValue<?> value:getAllValues())if(value.getCategory().equals(category)&&value.getName().equals(key))return value;return null;}

    public abstract class CValue<V> {
        @Nullable protected Property property;
        protected final String name;
        private final String[] comments;
        private final String localCategory;
        protected CValue(String name,String... comments){
            if(name==null||!name.matches("[A-Za-z0-9_\\-]+"))throw new IllegalArgumentException("Invalid config key: "+name);
            this.name=name;this.comments=comments.clone();this.localCategory=join(groups);allValues.add(this);
        }
        private void register(Configuration config,String prefix){String category=localCategory.isEmpty()?prefix:prefix+"."+localCategory;property=createProperty(config,category,joinComments(comments));}
        protected abstract Property createProperty(Configuration config,String category,String comment);
        protected final Property property(){if(property==null)throw new IllegalStateException("Config "+name+" was accessed before registration");return property;}
        public abstract V get();
        public final void set(V value){validate(value);write(value);save();}
        public abstract V getDefault();
        public abstract void setSerialized(String value);
        protected abstract void write(V value);
        protected void validate(V value){if(value==null)throw new IllegalArgumentException("Config value cannot be null");}
        public String getName(){return name;}
        public String[] getComments(){return comments.clone();}
        public String getComment(){return joinComments(comments);}
        @Nullable public Property getProperty(){return property;}
        public String getCategory(){String root=bindingPrefix==null?getName():bindingPrefix;return localCategory.isEmpty()?root:root+"."+localCategory;}
        public String serialize(){return String.valueOf(get());}
        public boolean isDefault(){return getDefault().equals(get());}
        public void reset(){set(getDefault());}
    }

    public final class ConfigGroup {
        private final String name;
        private final int groupDepth;
        private final String comment;
        private final String localCategory;
        ConfigGroup(String name,int groupDepth,String... comments){
            if(name==null||!name.matches("[A-Za-z0-9_\\-]+"))throw new IllegalArgumentException("Invalid config group: "+name);
            if(groupDepth<1)throw new IllegalArgumentException("group depth must be at least 1");
            this.name=name;this.groupDepth=groupDepth;this.comment=joinComments(comments);
            while(groups.size()>=groupDepth)groups.remove(groups.size()-1);
            while(groups.size()<groupDepth-1)groups.add("group"+groups.size());
            groups.add(name);depth=groupDepth;localCategory=join(groups);
        }
        public String getName(){return name;}
        public int getDepth(){return groupDepth;}
        public String getComment(){return comment;}
        public String getLocalCategory(){return localCategory;}
        public String getCategory(){return bindingPrefix==null?localCategory:bindingPrefix+"."+localCategory;}
    }
    public final class ConfigBool extends CValue<Boolean> {
        ConfigBool(String name,boolean value,String... comment){super(name,comment);defaultValue=value;}
        private final boolean defaultValue;
        protected Property createProperty(Configuration c,String category,String comment){return c.get(category,name,defaultValue,comment);}
        public Boolean get(){return property().getBoolean(defaultValue);} protected void write(Boolean value){property().set(value);}
        public Boolean getDefault(){return defaultValue;}
        public void setSerialized(String value){if(!"true".equalsIgnoreCase(value)&&!"false".equalsIgnoreCase(value))throw new IllegalArgumentException("Expected true or false");set(Boolean.parseBoolean(value));}
    }
    public final class ConfigInt extends CValue<Integer> {
        private final int defaultValue,min,max;
        ConfigInt(String name,int value,int min,int max,String... comment){super(name,comment);if(min>max||value<min||value>max)throw new IllegalArgumentException("Invalid integer range/default");defaultValue=value;this.min=min;this.max=max;}
        protected Property createProperty(Configuration c,String category,String comment){return c.get(category,name,defaultValue,comment,min,max);}
        public Integer get(){return Math.max(min,Math.min(max,property().getInt(defaultValue)));} protected void validate(Integer value){super.validate(value);if(value<min||value>max)throw new IllegalArgumentException("Value must be between "+min+" and "+max);} protected void write(Integer value){property().set(value);}
        public Integer getDefault(){return defaultValue;}
        public void setSerialized(String value){try{set(Integer.parseInt(value));}catch(NumberFormatException e){throw new IllegalArgumentException("Expected an integer",e);}}
        public int getMin(){return min;} public int getMax(){return max;}
    }
    public final class ConfigFloat extends CValue<Double> {
        private final double defaultValue,min,max;
        ConfigFloat(String name,float value,float min,float max,String... comment){super(name,comment);if(Float.isNaN(value)||min>max||value<min||value>max)throw new IllegalArgumentException("Invalid float range/default");defaultValue=value;this.min=min;this.max=max;}
        protected Property createProperty(Configuration c,String category,String comment){return c.get(category,name,defaultValue,comment,min,max);}
        public Double get(){return Math.max(min,Math.min(max,property().getDouble(defaultValue)));} public float getF(){return get().floatValue();}
        public Double getDefault(){return defaultValue;}
        protected void validate(Double value){super.validate(value);if(value.isNaN()||value<min||value>max)throw new IllegalArgumentException("Value must be between "+min+" and "+max);} protected void write(Double value){property().set(value);}
        public void setSerialized(String value){try{set(Double.parseDouble(value));}catch(NumberFormatException e){throw new IllegalArgumentException("Expected a number",e);}}
        public double getMin(){return min;} public double getMax(){return max;}
    }
    public final class ConfigString extends CValue<String> {
        private final String defaultValue;
        ConfigString(String name,String value,String... comment){super(name,comment);defaultValue=value;}
        protected Property createProperty(Configuration c,String category,String comment){return c.get(category,name,defaultValue,comment);}
        public String get(){return property().getString();} protected void validate(String value){super.validate(value);if(value.length()>32767)throw new IllegalArgumentException("String is too long");} protected void write(String value){property().set(value);}
        public String getDefault(){return defaultValue;}
        public void setSerialized(String value){set(value);}
    }
    public final class ConfigEnum<T extends Enum<T>> extends CValue<T> {
        private final T defaultValue;private final T[] values;
        ConfigEnum(String name,T value,String... comment){super(name,comment);defaultValue=value;values=value.getDeclaringClass().getEnumConstants();}
        protected Property createProperty(Configuration c,String category,String comment){String[] valid=new String[values.length];for(int i=0;i<values.length;i++)valid[i]=values[i].name();return c.get(category,name,defaultValue.name(),comment,valid);}
        public T get(){String raw=property().getString();for(T value:values)if(value.name().equalsIgnoreCase(raw))return value;return defaultValue;}
        public T getDefault(){return defaultValue;}
        protected void write(T value){property().set(value.name());}
        public void setSerialized(String raw){for(T value:values)if(value.name().equalsIgnoreCase(raw)){set(value);return;}throw new IllegalArgumentException("Unknown value "+raw);}
        public T[] getValues(){return values.clone();}
    }
    private static String join(List<String> groups){StringBuilder result=new StringBuilder();for(String group:groups){if(result.length()>0)result.append('.');result.append(group);}return result.toString();}
    private static String joinComments(String[] comments){StringBuilder result=new StringBuilder();for(String comment:comments){if(result.length()>0)result.append('\n');result.append(comment);}return result.toString();}
}
