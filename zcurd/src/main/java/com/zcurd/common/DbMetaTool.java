package com.zcurd.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.jfinal.plugin.activerecord.Record;
import com.zcurd.model.ZcurdField;
import com.zcurd.model.ZcurdHead;
import com.zcurd.model.ZcurdHeadBtn;
import com.zcurd.model.ZcurdHeadJs;
import com.zcurd.vo.ZcurdMeta;

public class DbMetaTool {
	
	private static Map<Integer, ZcurdMeta> metaDataMap = new Hashtable<Integer, ZcurdMeta>();

	public static ZcurdMeta getMetaData(int headId) {
		ZcurdMeta metaData = metaDataMap.get(headId);
		if(metaData == null) {
			metaData = getMetaDataFromDb(headId);
			metaDataMap.put(headId, metaData);
		}
		return metaData;
	}
	
	private static ZcurdMeta getMetaDataFromDb(int headId) {
		ZcurdHead head = ZcurdHead.me.findById(headId);
		List<ZcurdField> fieldList = ZcurdField.me.findByHeadId(head.getLong("id").intValue());
		
		Map<String, Map<String, Object>> dictMap = new HashMap<String, Map<String, Object>>();
		for (ZcurdField zcurdField : fieldList) {
			String dictSql = zcurdField.getStr("dict_sql");
			if(StringUtil.isNotEmpty(dictSql)) {
				Map<String, Object> dictData = getDictData(dictSql);
				dictMap.put(zcurdField.getStr("field_name"), dictData);
				zcurdField.put("dict", dictData);
			}
		}
		List<ZcurdField> addFieldList = new ArrayList<ZcurdField>();
		List<ZcurdField> updateFieldList = new ArrayList<ZcurdField>();
		List<ZcurdField> footerFieldList = new ArrayList<ZcurdField>();
		for (ZcurdField zcurdField : fieldList) {
			if(zcurdField.getIsShowList() == 1 && zcurdField.getIsSum() == 1) {
				footerFieldList.add(zcurdField);
			}
			
			if(!zcurdField.getStr("field_name").equals(head.getStr("id_field"))) {
				if(zcurdField.getInt("is_allow_add") == 1) {
					addFieldList.add(zcurdField);
				}
				if(zcurdField.getInt("is_allow_update") == 1 || zcurdField.getInt("is_allow_detail") == 1) {
					updateFieldList.add(zcurdField);
				}
			}
		}
		List<ZcurdHeadBtn> btnList = ZcurdHeadBtn.me.findByHeadId(headId);
		List<ZcurdHeadBtn> topList = new ArrayList<ZcurdHeadBtn>(); 
		List<ZcurdHeadBtn> lineList = new ArrayList<ZcurdHeadBtn>(); 
		for (ZcurdHeadBtn btn : btnList) {
			if(btn.getInt("location") == 1) {
				topList.add(btn);
			}else if(btn.getInt("location") == 2) {
				lineList.add(btn);
			}
		}
		for (ZcurdHeadBtn btn : btnList) {
			if(btn.getInt("action") == 1) {
				head.set("form_type", 2); //设置表单类型为主从
				break;
			}
		}
		List<ZcurdHeadJs> jsList = ZcurdHeadJs.me.findByHeadId(headId);
		
		ZcurdMeta zcurdMeta = new ZcurdMeta();
		zcurdMeta.setHead(head);
		zcurdMeta.setFieldList(fieldList);
		zcurdMeta.setDictMap(dictMap);
		zcurdMeta.setAddFieldList(addFieldList);
		zcurdMeta.setUpdateFieldList(updateFieldList);
		zcurdMeta.setFooterFieldList(footerFieldList);
		zcurdMeta.setBtnList(btnList);
		zcurdMeta.setTopList(topList);
		zcurdMeta.setLineList(lineList);
		zcurdMeta.setJsList(jsList);
		return zcurdMeta;
	}
	
	public static void updateMetaData(int headId) {
		metaDataMap.remove(headId);
	}
	
	/**
	 * 根据字典sql获得字典数据
	 */
	public static Map<String, Object> getDictData(String dictSql) {
		String[] parseSql = DBTool.parseSQL4DbSource(dictSql);
		
		Map<String, Object> map = new HashMap<String, Object>();
		List<Record> listRecord = DBTool.use(parseSql[0]).find("select 'key', 'text' union all select * from (" + parseSql[1] + ") a");
		for (int i = 1; i < listRecord.size(); i++) {
			Record record = listRecord.get(i);
			map.put(record.getStr("key"), record.getStr("text"));
		}
		return map;
	}
	
}
