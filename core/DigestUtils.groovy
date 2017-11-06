#! /usr/bin/env groovy


import java.security.MessageDigest

def md5(String s){
    MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex().toString()
}
