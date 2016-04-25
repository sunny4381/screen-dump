## 実行方法


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

※Java は 1.8 が必要。


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
  * AJAX の動作が遅いサイトの場合、長めに。
