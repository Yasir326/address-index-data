#!/usr/bin/python
# -*- coding: utf-8 -*-
"""
ONS Address Index - Probabilistic Parser
========================================

This file defines the Conditional Random Field parser settings including output file,
structure of the XML expected to hold training data, tokens and features.


Training
--------

Command line:
    parserator train training/training.xml addressParser


Requirements
------------

:requires: parserator
:requires: pycrfsuite
:requires: pandas


Author
------

:author: Sami Niemi (sami.niemi@valtech.co.uk)


Version
-------

:version: 0.2
:date: 17-Oct-2016
"""
import pycrfsuite
import os
import re
import warnings
import string
from collections import OrderedDict
import pandas as pd


# filename for the crfsuite settings file
MODEL_FILE = 'addresses.crfsuite'

#  _____________________
# |1. CONFIGURE LABELS! |
# |_____________________|

LABELS = ['OrganisationName',
          'DepartmentName',
          'SubBuildingName',
          'BuildingName',
          'BuildingNumber',
          'StreetName',
          'Locality',
          'TownName',
          'Postcode']

# set the XML segment names
PARENT_LABEL = 'AddressString'              # the XML tag for each labeled string
GROUP_LABEL = 'AddressCollection'           # the XML tag for a group of strings
NULL_LABEL = 'Null'                         # the null XML tag

# set some features that are being used to identify tokens
DIRECTIONS = {'n', 's', 'e', 'w', 'ne', 'nw', 'se', 'sw', 'north', 'south', 'east', 'west',
              'northeast', 'northwest', 'southeast', 'southwest'}
FLAT = {'FLAT', 'FLT', 'APARTMENT', 'APPTS', 'APPT' 'APTS', 'APT',
        'ROOM', 'ANNEX', 'ANNEXE', 'UNIT', 'BLOCK', 'BLK'}
COMPANY = {'CIC', 'CIO', 'LLP', 'LP', 'LTD', 'LIMITED', 'CYF', 'PLC', 'CCC', 'UNLTD', 'ULTD'}
ROAD = {'ROAD', 'RAOD', 'RD', 'DRIVE', 'DR', 'STREET', 'STRT', 'AVENUE','AVENEU', 'SQUARE',
        'LANE', 'LNE', 'LN', 'COURT', 'CRT', 'CT', 'PARK', 'PK', 'GRDN', 'GARDEN', 'CRESCENT',
        'CLOSE', 'CL', 'WALK', 'WAY', 'TERRACE', 'BVLD'}
Residential = {'HOUSE', 'HSE', 'FARM', 'LODGE', 'COURT', 'COTTAGE',
               'COTTAGES', 'VILLA', 'VILLAS', 'MAISONETTE', 'MEWS'}
Business = {'OFFICE', 'HOSPITAL', 'CARE', 'CLUB', 'BANK', 'BAR', 'UK', 'SOCIETY'}
Locational = {'BASEMENT', 'GROUND', 'UPPER', 'ABOVE', 'TOP', 'LOWER', 'FLOOR',
              'FIRST', '1ST', 'SECOND', '2ND', 'THIRD', '3RD', 'FOURTH', '4TH'}

# get some extra info - possible incodes and the linked post towns
df = pd.read_csv('/Users/saminiemi/Projects/ONS/AddressIndex/data/postcode_district_to_town.csv')
OUTCODES = set(df['postcode'].values)
POSTTOWNS = set(df['town'].values)
# county?


try:
    TAGGER = pycrfsuite.Tagger()
    TAGGER.open(os.path.split(os.path.abspath(__file__))[0] + '/' + MODEL_FILE)
except IOError:
    TAGGER = None
    warnings.warn(
        'You must train the model (parserator train [traindata] [modulename]) to create the %s file before you can use the parse and tag methods' % MODEL_FILE)


def parse(raw_string):
    if not TAGGER:
        raise IOError(
            '\nMISSING MODEL FILE: %s\nYou must train the model before you can use the parse and tag methods\nTo train the model annd create the model file, run:\nparserator train [traindata] [modulename]' % MODEL_FILE)

    tokens = tokenize(raw_string)
    if not tokens:
        return []

    features = tokens2features(tokens)

    tags = TAGGER.tag(features)
    return list(zip(tokens, tags))


def tag(raw_string):
    tagged = OrderedDict()

    for token, label in parse(raw_string):
        tagged.setdefault(label, []).append(token)

    for token in tagged:
        component = ' '.join(tagged[token])
        component = component.strip(' ,;')
        tagged[token] = component

    return tagged


#  _____________________
# |2. CONFIGURE TOKENS! |
# |_____________________| 
def tokenize(raw_string):
    # this determines how any given string is split into its tokens
    # handle any punctuation you want to split on, as well as any punctuation to capture

    if isinstance(raw_string, bytes):
        try:
            raw_string = str(raw_string, encoding='utf-8')
        except:
            raw_string = str(raw_string)

    re_tokens = re.compile(r"\(*\b[^\s,;#&()]+[.,;)\n]* | [#&]", re.VERBOSE | re.UNICODE)
    tokens = re_tokens.findall(raw_string)

    if not tokens:
        return []
    return tokens


#  _______________________
# |3. CONFIGURE FEATURES! |
# |_______________________| 
def tokens2features(tokens):
    # this should call tokenFeatures to get features for individual tokens,
    # as well as define any features that are dependent upon tokens before/after

    feature_sequence = [tokenFeatures(tokens[0])]
    previous_features = feature_sequence[-1].copy()

    for token in tokens[1:]:
        # set features for individual tokens (calling tokenFeatures)
        token_features = tokenFeatures(token)
        current_features = token_features.copy()

        # features for the features of adjacent tokens
        feature_sequence[-1]['next'] = current_features
        token_features['previous'] = previous_features

        # DEFINE ANY OTHER FEATURES THAT ARE DEPENDENT UPON TOKENS BEFORE/AFTER
        # for example, a feature for whether a certain character has appeared previously in the token sequence

        feature_sequence.append(token_features)
        previous_features = current_features

    if len(feature_sequence) > 1:
        # these are features for the tokens at the beginning and end of a string
        feature_sequence[0]['rawstring.start'] = True
        feature_sequence[-1]['rawstring.end'] = True
        feature_sequence[1]['previous']['rawstring.start'] = True
        feature_sequence[-2]['next']['rawstring.end'] = True

    else:
        # a singleton feature, for if there is only one token in a string
        feature_sequence[0]['singleton'] = True

    return feature_sequence


def tokenFeatures(token):

    token_clean = token.upper()

    features = {'digits': digits(token_clean),
                'word': (token_clean if not token_clean.isdigit() else False),
                'length': (u'd:' + str(len(token_clean)) if token_clean.isdigit() else u'w:' + str(len(token_clean))),
                'endsinpunc': (token[-1] if bool(re.match('.+[^.\w]', token, flags=re.UNICODE)) else False),
                'directional': token_clean in DIRECTIONS,
                'outcode': token_clean in OUTCODES,
                'posttown': token_clean in POSTTOWNS,
                'has.vowels': bool(set(token_clean) & set('AEIOU')),
                'flat': token_clean in FLAT,
                'company': token_clean in COMPANY,
                'road': token_clean in ROAD,
                'residential': token_clean in Residential,
                'business': token_clean in Business,
                'locational': token_clean in Locational
                # how many dashes, like 127-129 or bradford-on-avon
                }

    return features


# get the casing of a token
def casing(token):
    if token.isupper():
        return 'upper'
    elif token.islower():
        return 'lower'
    elif token.istitle():
        return 'title'
    elif token.isalpha():
        return 'mixed'
    else:
        return False


def digits(token):
    if token.isdigit():
        return 'all_digits'
    elif set(token) & set(string.digits):
        return 'some_digits'
    else:
        return 'no_digits'


def trailingZeros(token):
    results = re.findall(r'(0+)$', token)
    if results:
        return results[0]
    else:
        return ''
