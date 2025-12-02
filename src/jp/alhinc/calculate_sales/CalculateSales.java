package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String FILE_NOT_SEQUENTIAL_NUMBER = "売上ファイル名が連番になっていません";
	private static final String AMOUNT_OVER_10_DIGET = "合計金額が10桁を超えました";
	private static final String SALES_FILE_INVALID_BRANCH_CODE = "の支店コードが不正です";
	private static final String SALES_FILE_INVALID_FORMAT = "のフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {

		//エラー処理3-1
		//コマンドライン引数が渡されていない場合、処理終了
		if(args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// 処理内容2-1
		File[] files = new File(args[0]).listFiles();
		// ファイルを保持するArrayListの定義
		List<File> rcdFiles = new ArrayList<>();

		for(int i = 0; i < files.length; i++) {
			//ファイル名が「数字8桁.rcd」であるか判定し、ArrayListに追加
			if (files[i].getName().matches("^[0-9]{8}\\.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		// エラー処理2-1
		// 昇順にソート
		Collections.sort(rcdFiles);

		// 前後のファイルを比較（繰り返し回数はファイルのリスト数-1）
		for(int i = 0; i < rcdFiles.size() - 1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0,8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0,8));

		// 前後のファイルが連番になっていない場合、処理終了
			if((latter - former) != 1) {
				System.out.println(FILE_NOT_SEQUENTIAL_NUMBER);
				return;
			}
		}

		// 処理内容2-2
		for(int i = 0; i < rcdFiles.size(); i++) {

			// BufferedReaderの初期化
			BufferedReader br = null;

			try {
				File file = rcdFiles.get(i);
				FileReader fr = new FileReader(file);
				br = new BufferedReader(fr);

				String line;

				// 売上ファイルの中身を保持するArrayListの定義
				List<String> salesRecord = new ArrayList<String>();

				// ファイルの中身の読込、ArrayListに追加 ([0]に支店コード、[1]に売上金額)
				while((line = br.readLine()) != null) {
					salesRecord.add(line);
				}

				// エラー処理2-4
				// 売上ファイルの中身が2行ではない場合、処理終了
				if(salesRecord.size() != 2) {
					System.out.println(file.getName() + SALES_FILE_INVALID_FORMAT);
					return;
				}

				// エラー処理2-3
				// 売上ファイルの支店コードが支店定義ファイルに該当しない場合、処理終了
				if(!branchNames.containsKey(salesRecord.get(0))) {
					System.out.println(file.getName() + SALES_FILE_INVALID_BRANCH_CODE);
					return;
				}

				// エラー処理3-2
				//売上ファイルの売上金額が数字で無い場合、処理終了
				if(!salesRecord.get(1).matches("^[0-9]+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				// 売上金額の型の変換(String→long)
				long fileSale = Long.parseLong(salesRecord.get(1));
				// 支店コードの変数を宣言
				String branchCode = salesRecord.get(0);
				// 読み込んだ売上金額を対象の支店の売上金額合計に加算
				Long saleAmount = branchSales.get(branchCode) + fileSale;

				// エラー処理2-2
				// 売上金額合計が10桁を超える（11桁以上）の場合、処理終了
				if(saleAmount >= 10000000000L) {
					System.out.println(AMOUNT_OVER_10_DIGET);
					return;
				}

				// Mapに値を追加
				branchSales.put(branchCode, saleAmount);

			} catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;

			} finally {
				if(br != null) {
					try {
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}


		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);

			// エラー処理1-1
			// 支店名義ファイルが存在しない場合、処理終了
			if (!file.exists()) {
				System.out.println(FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				// 処理内容1-2
				//「,」で文字列を分割
				String[] items = line.split(",");

				// エラー処理1-2
				// フォーマットが不正な場合、処理終了
				if((items.length != 2) || (!items[0].matches("^[0-9]{3}$"))) {
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}

				//「,」より前(items[0])の変数を宣言
				String branchCode = items[0];
				//「,」より後(items[1])の変数を宣言
				String branchName = items[1];

				// Mapに値を追加
				branchNames.put(branchCode, branchName);
				branchSales.put(branchCode, 0L);
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;

		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		// 処理内容3-1
		//BufferedWriterの初期化
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			// 2つのマップから全てのKeyを取得
			for(String key : branchNames.keySet()) {
				// 支店コード、支店名、売上金額の変数を宣言
				String branchCode = key;
				String branchName = branchNames.get(key);
				Long saleAmount = branchSales.get(key);
				//値を書き出す
				bw.write(branchCode + "," + branchName + "," + saleAmount);
				// 改行
				bw.newLine();
			}


		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			if(bw != null) {
				try {
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}

		return true;
	}

}
