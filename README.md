## 実行方法

2 つのモードがある。

* URL リストからスクリーンショットを作成する
* URL リストをトラバースしながらスクリーンショットを作成する

※Java は 1.8 が必要。

### URL リストからスクリーンショットを作成する方法

スクリーンショットを取りたい URL を記載したテキストファイルを作成する。

```
http://demo.ss-proj.org/
http://demo.ss-proj.org/inquiry/
http://demo.ss-proj.org/board/
http://demo.ss-proj.org/docs/
http://demo.ss-proj.org/docs/page1.html
```

このファイルを `url.txt` とする。
次のコマンドでスクリーンショットを撮影する。

```
java -jar screen-dump-1.0-jar-with-dependencies.jar -b ブラウザ -o 出力ディレクトリ url.txt
```

### URL リストをトラバースしながらスクリーンショットを作成する方法

定義ファイルを作成する。定義ファイルは次のような yaml ファイルである。

```
traverse:
  seeds:
    - http://demo.ss-proj.org/
    - http://demo.ss-proj.org/inquiry/
    - http://demo.ss-proj.org/board/
    - http://demo.ss-proj.org/docs/
    - http://demo.ss-proj.org/docs/page1.html
  accessible_domains:
    - demo.ss-proj.org
  extractable_domains:
    - demo.ss-proj.org
  exclude_paths:
    - /.voice/
    - /mobile/
    - /#/kana/
  allow_suffixes:
    - /
    - .html
    - .xml
```

このファイルを `traverse.yml` とする。
次のコマンドでスクリーンショットを撮影する。

```
java -jar screen-dump-1.0-jar-with-dependencies.jar -b ブラウザ -o 出力ディレクトリ -t traverse.yml -l 1000
```


### オプション

指定できるオプションは:

* -b: ブラウザ
  * ie: Internet Explorer
  * firefox: Firefox
  * chrome: Google Chrome
  * edge: Microsoft Edge
* -o: 出力ディレクトリ
* -i: 初期スリープ（ミリ秒）
  * 最初に Basic 認証のユーザーID とパスワードを入れなければいけない場合は長めに。
* -s: スリープ（ミリ秒）
  * スクリーンショットをとるごとの wait
  * AJAX の動作が遅く、画面の表示に時間がかかるサイトの場合、長めに。
* -l: 制限
  * 指定された数のスクリーンショットを撮影するとプログラムは終了する。
* -t: トラバースファイル
* --width: ウィンドウ幅
* --height: ウィンドウ高さ
