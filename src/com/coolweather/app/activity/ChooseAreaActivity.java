package com.coolweather.app.activity;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import com.coolweather.app.R;
import com.coolweather.app.model.City;
import com.coolweather.app.model.CoolWeatherDB;
import com.coolweather.app.model.Country;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
 
public class ChooseAreaActivity extends Activity {

	public static final int LEVEL_PROVINCE=0;
	public static final int LEVEL_CITY=1;
	public static final int LEVEL_COUNTRY=2;
	
	private TextView titleText;
	private ListView listView;
	private ProgressDialog progressDialog;
	private List<String> dataList=new ArrayList<String>();
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<Province> provinceList;
	private List<City> cityList;
	private List<Country> countryList;
	private Province selectedProvince;
	private City selectedCity;
	private int currentLevel;
	private boolean isFromWeatherActivity;//是否从WeatherActivity中跳转过来
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		isFromWeatherActivity=getIntent().getBooleanExtra("from_weather_activity", false);
		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(ChooseAreaActivity.this);
		
		if (prefs.getBoolean("city_selected", false)&&!isFromWeatherActivity) {
			Intent intent=new Intent(ChooseAreaActivity.this,WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView=(ListView) findViewById(R.id.list_view);
		titleText=(TextView) findViewById(R.id.title_text);
		adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
		listView.setAdapter(adapter);
		coolWeatherDB=CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				if (currentLevel==LEVEL_PROVINCE) {
					selectedProvince=provinceList.get(arg2);
					queryCities();
				}else if (currentLevel==LEVEL_CITY) {
					selectedCity=cityList.get(arg2);
					queryCountries();
				}else if (currentLevel==LEVEL_COUNTRY) {
					String countryCode=countryList.get(arg2).getCountryCode();
					Intent intent=new Intent(ChooseAreaActivity.this,WeatherActivity.class);
					intent.putExtra("country_code", countryCode);
					startActivity(intent);
					finish();
				}
			}
		});
		queryProvinces();
	}
	
	private void queryProvinces(){
		provinceList=coolWeatherDB.queryProvince();
		if (provinceList.size()>0) {
			dataList.clear();
			for(Province province:provinceList){
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel=LEVEL_PROVINCE;
		}else {
			queryFromServer(null, "Province");
		}
	}
	
	private void queryCities(){
		cityList=coolWeatherDB.queryCity(selectedProvince.getId());
		if (cityList.size()>0) {
			dataList.clear();
			for (City city:cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel=LEVEL_CITY;
		}else {
			queryFromServer(selectedProvince.getProvinceCode(), "City");
		}
	}
	
	private void queryCountries(){
		countryList=coolWeatherDB.queryCountry(selectedCity.getId());
		if (countryList.size()>0) {
			dataList.clear();
			for(Country country:countryList){
				dataList.add(country.getCountryName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel=LEVEL_COUNTRY;
		}else {
			queryFromServer(selectedCity.getCityCode(), "Country");
		}
	}
	
	private void queryFromServer(final String code,final String type){
		String address;
		if (!TextUtils.isEmpty(code)) {
			address="http://www.weather.com.cn/data/list3/city"+code+".xml";
		}else {
			address="http://www.weather.com.cn/data/list3/city.xml";
		}
		showDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				// TODO Auto-generated method stub
				boolean result=false;
				if ("Province".equals(type)) {
					result=Utility.handleProvincesResponse(coolWeatherDB, response);
				}else if ("City".equals(type)) {
					result=Utility.handleCitiesResponse(coolWeatherDB, response, selectedProvince.getId());
				}else if ("Country".equals(type)) {
					result=Utility.handleCountriesResponse(coolWeatherDB, response, selectedCity.getId());
				}
				
				if (result) {
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							closeProgressDialog();
							if ("Province".equals(type)) {
								queryProvinces();
							}else if ("City".equals(type)) {
								queryCities();
							}else if ("Country".equals(type)) {
								queryCountries();
							}
						}
					});
				}
			}
			
			@Override
			public void onError(Exception e) {
				// TODO Auto-generated method stub
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}
	
	private void showDialog(){
		if (progressDialog==null) {
			progressDialog=new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
			//dialog.setCancelable(false);
			//dialog弹出后会点击屏幕或物理返回键，dialog不消失

			//dialog.setCanceledOnTouchOutside(false);
			//dialog弹出后会点击屏幕，dialog不消失；点击物理返回键dialog消失
		}
		progressDialog.show();
	}
	
	private void closeProgressDialog(){
		if (progressDialog!=null) {
			progressDialog.dismiss();
		}
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		if (currentLevel==LEVEL_COUNTRY) {
			queryCities();
		}else if (currentLevel==LEVEL_CITY) {
			queryProvinces();
		}else {
			if (isFromWeatherActivity) {
				Intent intent=new Intent(ChooseAreaActivity.this,WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}
	}
}
