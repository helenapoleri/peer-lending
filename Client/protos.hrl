%% -*- coding: utf-8 -*-
%% Automatically generated, do not edit
%% Generated by gpb_compile version 4.4.0

-ifndef(protos).
-define(protos, true).

-define(protos_gpb_version, "4.4.0").

-ifndef('LOGINREQ_PB_H').
-define('LOGINREQ_PB_H', true).
-record('LoginReq',
        {name                   :: iolist(),        % = 1
         password               :: iolist()         % = 2
        }).
-endif.

-ifndef('LOGINREP_PB_H').
-define('LOGINREP_PB_H', true).
-record('LoginRep',
        {cType                  :: 'COMPANY' | 'INVESTOR' | integer() | undefined, % = 1, enum LoginRep.ClientType
         status                 :: 'INVALID' | 'SUCCESS' | integer() % = 2, enum LoginRep.Status
        }).
-endif.

-endif.
