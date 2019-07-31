# WSN_Sandbox

Skripsi Jonathan Alva / 2015730047

Judul : Pengembangan Aplikasi Transfer Data di WSN

Dibimbing Oleh : Elisati Hulu M.T

================================================================================
Pada Git ini terdapat Dokumen Skripsi, Dan Kode Program Aplikasi WSN.

Cara Menggunakan Program:

1. Hal yang perlu di instal:

  a. Eclipse IDE
  
  b. Java JDK & JRE
  
  c. Jar Apache Ant
  
  
2. Perangkat Keras Yang Diperlukan:

  a. Node Sensor Preon32.
  
  b. Baterai 9v
  
  c. Kabel USB Tipe A
  
  
3. Langkah Instalasi

  a. Siapkan semua program yang dibutuhkan pada komputer.
  
  b. Aturlah PATH yang digunakan pada system variable komputer anda (Ant, Java jdk, jre, eclipse)
  
  c. Buka Eclipse IDE
  
  d. Masukan 2 Project ke dalam workspace pada Eclipse.
  
  e. Project pertama yaitu Handler. Project ini digunakan untuk menangani program yang memiliki hubungan antara Base Station dengan Komputer pengguna.
  
  f. Project pertama yaitu Sandbox. Pada project ini yang menangani pemrograman pada setiap node sensor.


4. Langkah Menambahkan dan Mengatur Context

Context digunakan untuk mengatur setiap node sensor menyimpan kode program.

  a. Pada folder Sandbox file "buildUser.xml" tambahkan context baru dengan:

	<target name="context.set.1">
		<switchContext to="config/context1.properties" />
	</target>

  b. Pada folder Sandbox/config, buat file sesuai dengan switchContext pada nomor 1 (context1.properties)
  
  c. Atur context sesuai dengan nama class, nama module, dan nama port yang digunakan untuk melakukan transfer data ke node sensor.
  
  d. Jika menggunakan baterai maka module.name harus diisi dengan "autostart", namun jika menjalankan program node sensor melalui ant script maka module.name harus diisi dengan nama lain selain "autostart"
  

5. Langkah Mengatur Address Base Station

  a. Pada folder SandBox/src/defaultpackage/BS.java terdapat beberapa atribut yang dapat disesuaikan dengan kebutuhkan.
  
  b. node_list = Atribut ini digunakan untuk mengatur node-node yang ada pada jaringan.
  
  c. ADDR_NODE3 = Atribut ini digunakan untuk mengatur alamat dari base station.
  
  d. ADDR_NODE2 = Atribut ini digunakan untuk mengatur node yang terhubung langsung dengan base station
  
  e. Daftar alamat yang dapat langsung digunakan terdapat pada file Address_Base.txt
  

6. Langkah Mengatur Address Node Sensor

  a. Pada folder SandBox/src/defaultpackage/BS.java terdapat beberapa atribut yang dapat disesuaikan dengan kebutuhkan.
  
  b. node_list = Atribut ini digunakan untuk mengatur node-node yang ada pada jaringan.
  
  c. ADDR_NODE1 = Atribut ini digunakan untuk mengatur alamat node diatas node tersebut. Jika tidak ada maka dapat diisi "new int[0]"
  
  d. ADDR_NODE2 = Atribut ini digunakan untuk mengatur alamat node yang terhubung dengan node tersebut dan sebagai tujuan pengiriman perintah dari suatu node
  
  e. ADDR_NODE3 = Atribut ini digunakan untuk mengatur alamat dari node tersebut.
  
  f. Daftar alamat yang dapat langsung digunakan terdapat pada file Address_Node.txt
  

7. Langkah Mengunggah Program Ke Dalam Node Sensor.

  a. Atur context yang digunakan (nama kelas, module, dan comport yang digunakan).
  
  b. Pada menu ant script "Preon32 Sandbox User" pilih context yang telah di atur sebelumnya dengan cara klik 2x.
  
  c. Pada menu ant script "Preon32 Sandbox" pilih perintah ".all" untuk menunggah program.
  
  d. Dapat juga menggunakan perintah "cmd.module.upload", "cmd.module.run" secara berurutan (jika gagal gunakan cara nomor 3)
  
  e. Setelah program berhasil diunggah dan dijalankan maka akan muncul console sebagai tampilan pengguna.
  

8. Mengatur Handler

Handler digunakan sebagai perantara antara program komputer dengan base station. Handler harus diatur sesuai dengan port yang digunakan base station.

  a. Pada file handler.java terdapat beberapa hal yang harus diubah sesuai dengan kebutuhan.
  
  b. Pada method init() ubah nama port yang digunakan oleh base station. (Base station terhubung dengan port yang mana pada komputer).
Preon32Helper nodeHelper = new Preon32Helper("COM8", 115200); pada contoh tersebut maka base station terhubung dengan port COM8.

  c. Sesuaikan nama module yang telah diunggah ke dalam base station pada atribut "DataConnection conn" di method tersebut.
  

9. Menjalankan Program Secara Bersamaan.

  a. Pastikan program pada node sensor sudah berjalan. (Jika menggunakan baterai maka baterai harus sudah terpasang, dan module yang diunggah haruslah "autostart". Jika tidak menggunakan baterai, program harus dijalakan melalui ant script)
  
  b. Jalankan Program Handler.java
  
  c. Pilih menu yang telah disediakan.
  

10. Tambahan

  a. Setiap port pada komputer akan memiliki nama yang berbeda saat dicolok oleh node sensor yang berbeda. Ini harus diperhatikan, jika salah maka program tidak dapat diupload.
  
  b. Jika ada beberapa orang yang sedang menggunakan WSN pada waktu yang sama, maka haruslah memiliki PAN_ID yang berbeda agar tidak terjadi bentrok pada jaringan tersebut.
  
  c. Informasi lebih lanjut dapat menghubungi : Jonathan Alva (jonathan.alva97@yahoo.com)
  
